/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package deployments;


import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.connection.HostDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProxiedActiveFailoverDeployment extends ProxiedContainerDeployment {

    private final Logger log = LoggerFactory.getLogger(ProxiedActiveFailoverDeployment.class);
    private final Map<String, GenericContainer<?>> servers;
    private volatile Network network;
    private volatile ToxiproxyContainer toxiproxy;

    ProxiedActiveFailoverDeployment(int servers) {

        this.servers = IntStream.range(0, servers)
                .mapToObj(i -> "server" + i)
                .map(name -> new AbstractMap.SimpleEntry<String, GenericContainer<?>>(name, createServer(name)))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    }

    @Override
    public List<HostDescription> getHosts() {
        return getProxiedHosts().stream()
                .map(ProxiedHost::getHostDescription)
                .collect(Collectors.toList());
    }

    @Override
    public ArangoTopology getTopology() {
        return ArangoTopology.ACTIVE_FAILOVER;
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStart() {
        return CompletableFuture
                .runAsync(() -> {
                    network = Network.newNetwork();
                    servers.values().forEach(agent -> agent.withNetwork(network));
                    toxiproxy = new ToxiproxyContainer().withNetwork(network);
                })
                .thenCompose(__ -> CompletableFuture.runAsync(toxiproxy::start).thenAccept(___ -> log.info("READY: toxiproxy")))
                .thenCompose(__ -> CompletableFuture.allOf(
                        performActionOnGroup(servers.values(), GenericContainer::start)
                ))
                .thenCompose(__ -> {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    try {
                        for (GenericContainer<?> server : servers.values()) {
                            server.execInContainer(
                                    "arangosh",
                                    "--server.authentication=false",
                                    "--javascript.execute-string=require('org/arangodb/users').update('" + getUser() + "', '" + getPassword() + "')");
                        }
                        future.complete(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                    return future;
                })
                .thenAccept(__ -> servers.keySet().forEach(k -> toxiproxy.getProxy(k, 8529)))
                .thenCompose(__ -> CompletableFuture.runAsync(() -> ContainerUtils.waitForAuthenticationUpdate(this)))
                .thenAccept(__ -> log.info("Active Failover Deployment is ready!"))
                .thenApply(__ -> this);
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStop() {
        return CompletableFuture.allOf(
                performActionOnGroup(servers.values(), GenericContainer::stop)
        )
                .thenAcceptAsync(__ -> network.close())
                .thenAccept(__ -> log.info("Cluster has been shutdown!"))
                .thenApply(__ -> this);
    }

    private GenericContainer<?> createContainer(String name) {
        return new GenericContainer<>(getImage())
                .withEnv("ARANGO_LICENSE_KEY", getLicenseKey())
                .withCopyFileToContainer(MountableFile.forClasspathResource("deployments/jwtSecret"), "/jwtSecret")
                .withExposedPorts(8529)
                .withNetworkAliases(name)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[" + name + "]"))
                .waitingFor(Wait.forLogMessage(".*resilientsingle up and running.*", 1).withStartupTimeout(Duration.ofSeconds(60)));
    }

    private GenericContainer<?> createServer(String name) {
        String DOCKER_COMMAND = "arangodb --starter.address=$(hostname -i) --auth.jwt-secret /jwtSecret --starter.mode=activefailover --starter.join server1,server2,server3";
        return createContainer(name)
                .withCommand("sh", "-c", DOCKER_COMMAND);
    }

    private CompletableFuture<Void> performActionOnGroup(Collection<GenericContainer<?>> group, Consumer<GenericContainer<?>> action) {
        return CompletableFuture.allOf(
                group.stream()
                        .map(it -> CompletableFuture.runAsync(() -> action.accept(it)))
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    public List<ProxiedHost> getProxiedHosts() {
        return servers.keySet().stream()
                .map(genericContainer -> ProxiedHost.builder()
                        .proxiedHost(genericContainer)
                        .proxiedPort(8529)
                        .toxiproxy(toxiproxy)
                        .build())
                .collect(Collectors.toList());
    }

}
