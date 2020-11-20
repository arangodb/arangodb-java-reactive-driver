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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProxiedSingleServerDeployment extends ProxiedContainerDeployment {

    private static final Logger log = LoggerFactory.getLogger(ProxiedSingleServerDeployment.class);

    private final int PORT = 8529;
    private final ToxiproxyContainer toxiproxy;
    private final GenericContainer<?> container;
    private volatile Network network;
    private ToxiproxyContainer.ContainerProxy proxy;

    ProxiedSingleServerDeployment() {
        toxiproxy = new ToxiproxyContainer();
        container = new GenericContainer<>(getImage())
                .withEnv("ARANGO_LICENSE_KEY", getLicenseKey())
                .withEnv("ARANGO_ROOT_PASSWORD", getPassword())
                .withExposedPorts(PORT)
                .withNetworkAliases("db")
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[DB_LOG]"))
                .waitingFor(Wait.forHttp("/_api/version")
                        .withBasicCredentials(getUser(), getPassword())
                        .forStatusCode(200));
    }

    /**
     * call after {@link #start()}
     *
     * @return HostDescription of the proxy
     */
    @Override
    public List<HostDescription> getHosts() {
        return Collections.singletonList(HostDescription.of(proxy.getContainerIpAddress(), proxy.getProxyPort()));
    }

    @Override
    public ArangoTopology getTopology() {
        return ArangoTopology.SINGLE_SERVER;
    }

    @Override
    CompletableFuture<ProxiedContainerDeployment> asyncStart() {
        return CompletableFuture
                .runAsync(() -> {
                    network = Network.newNetwork();
                    toxiproxy.withNetwork(network);
                    container.withNetwork(network);
                })
                .thenCompose(__ -> CompletableFuture.allOf(
                        CompletableFuture.runAsync(container::start).thenAccept((v) -> log.info("READY: db")),
                        CompletableFuture.runAsync(toxiproxy::start).thenAccept((v) -> log.info("READY: toxiproxy"))
                ))
                .thenCompose(__ -> CompletableFuture.runAsync(() -> proxy = toxiproxy.getProxy("db", PORT)))
                .thenApply(__ -> this);
    }


    @Override
    CompletableFuture<ContainerDeployment> asyncStop() {
        return CompletableFuture.allOf(
                CompletableFuture.runAsync(container::stop).thenAccept((v) -> log.info("STOPPED: db")),
                CompletableFuture.runAsync(toxiproxy::stop).thenAccept((v) -> log.info("STOPPED: toxiproxy"))
        )
                .thenAcceptAsync(__ -> network.close())
                .thenApply(__ -> this);
    }

    @Override
    public List<ProxiedHost> getProxiedHosts() {
        return Collections.singletonList(ProxiedHost.builder()
                .proxiedHost("db")
                .proxiedPort(PORT)
                .toxiproxy(toxiproxy)
                .build());
    }

}
