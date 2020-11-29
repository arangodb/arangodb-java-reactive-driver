package deployments;


import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.connection.HostDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ActiveFailoverDeployment extends ContainerDeployment {

    private final Logger log = LoggerFactory.getLogger(ActiveFailoverDeployment.class);
    private final Map<String, GenericContainer<?>> servers;
    private volatile ReusableNetwork network;

    ActiveFailoverDeployment(int servers) {

        this.servers = IntStream.range(0, servers)
                .mapToObj(i -> "server" + i)
                .map(name -> new AbstractMap.SimpleEntry<String, GenericContainer<?>>(name, createServer(name)))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    }

    private CompletableFuture<Void> configureAuthentication() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            for (GenericContainer<?> server : servers.values()) {
                server.execInContainer(
                        "arangosh",
                        "--server.authentication=false",
                        "--javascript.execute-string=require('org/arangodb/users').update('" + getUser() + "', '" + getPassword() + "')");
            }
            future.complete(null);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> verifyAuthentication() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            for (GenericContainer<?> server : servers.values()) {
                Container.ExecResult result = server.execInContainer(
                        "arangosh",
                        "--server.username=" + getUser(),
                        "--server.password=" + getPassword(),
                        "--javascript.execute-string=db._version()");

                if (result.getExitCode() != 0) {
                    future.complete(false);
                }
            }

            future.complete(true);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private String getContainerIP(final GenericContainer<?> container) {
        return container.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .get(network.getName())
                .getIpAddress();
    }

    @Override
    public List<HostDescription> getHosts() {
        return servers.values().stream()
                .map(it -> HostDescription.of(getContainerIP(it), 8529))
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
                    network = ReusableNetwork.of("active-failover");
                    servers.values().forEach(agent -> agent.withNetwork(network));
                })
                .thenCompose(__ -> CompletableFuture.allOf(
                        performActionOnGroup(servers.values(), GenericContainer::start)
                ))
                .thenCompose(__ -> verifyAuthentication())
                .thenCompose(authenticationConfigured -> {
                    if (authenticationConfigured) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return configureAuthentication();
                    }
                })
                .thenCompose(__ -> CompletableFuture.runAsync(() -> ContainerUtils.waitForAuthenticationUpdate(this)))
                .thenAccept(__ -> log.info("Cluster is ready!"))
                .thenApply(__ -> this);
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStop() {
        if (isReuse() && isStarted()) {
            return CompletableFuture.completedFuture(this);
        }

        return CompletableFuture.allOf(
                performActionOnGroup(servers.values(), GenericContainer::stop)
        )
                .thenAcceptAsync(__ -> network.close())
                .thenAccept((v) -> log.info("Cluster has been shutdown!"))
                .thenApply((v) -> this);
    }

    private GenericContainer<?> createContainer(String name) {
        GenericContainer<?> c = new GenericContainer<>(getImage())
                .withReuse(isReuse())
                .withEnv("ARANGO_LICENSE_KEY", getLicenseKey())
                .withCopyFileToContainer(MountableFile.forClasspathResource("deployments/jwtSecret"), "/jwtSecret")
                .withExposedPorts(8529)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[" + name + "]"))
                .waitingFor(Wait.forLogMessage(".*resilientsingle up and running.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

        // .withNetworkAliases creates also a random alias, which prevents container reuse
        c.setNetworkAliases(Collections.singletonList(name));
        return c;
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

}
