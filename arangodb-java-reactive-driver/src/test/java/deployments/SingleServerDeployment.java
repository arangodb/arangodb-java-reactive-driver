package deployments;


import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.connection.HostDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SingleServerDeployment extends ContainerDeployment {

    private static final Logger log = LoggerFactory.getLogger(SingleServerDeployment.class);

    private final GenericContainer<?> container;

    SingleServerDeployment() {
        container = new GenericContainer<>(getImage())
                .withReuse(isReuse())
                .withEnv("ARANGO_LICENSE_KEY", getLicenseKey())
                .withEnv("ARANGO_ROOT_PASSWORD", getPassword())
                .withExposedPorts(8529)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[DB_LOG]"))
                .waitingFor(Wait.forLogMessage(".*ready for business.*", 1));
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
        if (isReuse() && isStarted()) {
            return CompletableFuture.completedFuture(this);
        }

        return CompletableFuture.runAsync(container::stop).thenAccept((v) -> log.info("Stopped!")).thenApply((v) -> this);
    }

}
