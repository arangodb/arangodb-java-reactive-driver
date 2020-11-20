package deployments;


import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.connection.HostDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class SingleServerSslDeployment extends ContainerDeployment {

    private static final Logger log = LoggerFactory.getLogger(SingleServerSslDeployment.class);
    private static final String command = "arangod --ssl.keyfile /server.pem --server.endpoint ssl://0.0.0.0:8529 ";

    private final GenericContainer<?> container;
    private String sslProtocol;

    SingleServerSslDeployment() {
        String SSL_CERT_PATH = Paths.get("../docker/server.pem").toAbsolutePath().toString();
        container = new GenericContainer<>(getImage())
                .withEnv("ARANGO_LICENSE_KEY", getLicenseKey())
                .withEnv("ARANGO_ROOT_PASSWORD", getPassword())
                .withExposedPorts(8529)
                .withFileSystemBind(SSL_CERT_PATH, "/server.pem", BindMode.READ_ONLY)
                .withCommand(command)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[DB_LOG]"))
                .waitingFor(Wait.forLogMessage(".*ready for business.*", 1));
    }

    /**
     * @param sslProtocol value from https://www.arangodb.com/docs/stable/programs-arangod-ssl.html#ssl-protocol
     */
    SingleServerSslDeployment(String sslProtocol) {
        this();
        this.sslProtocol = sslProtocol;
        container.withCommand(command + "--ssl.protocol " + sslProtocol);
    }

    @Override
    public List<HostDescription> getHosts() {
        return Collections.singletonList(HostDescription.of(container.getContainerIpAddress(), container.getFirstMappedPort()));
    }

    @Override
    public ArangoTopology getTopology() {
        return ArangoTopology.SINGLE_SERVER;
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStart() {
        return CompletableFuture.runAsync(container::start).thenAccept((v) -> log.info("Ready!")).thenApply((v) -> this);
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStop() {
        return CompletableFuture.runAsync(container::stop).thenAccept((v) -> log.info("Stopped!")).thenApply((v) -> this);
    }

    @Override
    protected String getSslProtocol() {
        return "6".equals(sslProtocol) ? "TLSv1.3" : "TLSv1.2";
    }

}
