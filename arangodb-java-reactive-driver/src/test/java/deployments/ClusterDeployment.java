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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClusterDeployment extends ContainerDeployment {

    private final Logger log = LoggerFactory.getLogger(ClusterDeployment.class);
    private final String DOCKER_COMMAND = "arangodb --auth.jwt-secret /jwtSecret ";
    private final List<GenericContainer<?>> agents;
    private final List<GenericContainer<?>> dbServers;
    private final Map<String, GenericContainer<?>> coordinators;
    private volatile ReusableNetwork network;

    ClusterDeployment(int dbServers, int coordinators) {

        agents = IntStream.range(0, 3)
                .mapToObj(this::createAgent)
                .collect(Collectors.toList());

        this.dbServers = IntStream.range(0, dbServers)
                .mapToObj(i -> "dbServer" + i)
                .map(this::createDbServer)
                .collect(Collectors.toList());

        this.coordinators = IntStream.range(0, coordinators)
                .mapToObj(i -> "coordinator" + i)
                .map(name -> new AbstractMap.SimpleEntry<String, GenericContainer<?>>(name, createCoordinator(name)))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    }

    private CompletableFuture<Boolean> verifyClusterAuthentication() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Container.ExecResult result = coordinators.values().iterator().next().execInContainer(
                    "arangosh",
                    "--server.username=" + getUser(),
                    "--server.password=" + getPassword(),
                    "--javascript.execute-string=db._version()");
            future.complete(result.getExitCode() == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Void> configureClusterAuthentication() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            Container.ExecResult result = coordinators.values().iterator().next().execInContainer(
                    "arangosh",
                    "--server.authentication=false",
                    "--javascript.execute-string=require('org/arangodb/users').update('" + getUser() + "', '" + getPassword() + "')");

            if (result.getExitCode() != 0) {
                throw new RuntimeException(result.getStderr() + "\n" + result.getStdout());
            }

            future.complete(null);
        } catch (Exception e) {
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
        return coordinators.values().stream()
                .map(it -> HostDescription.of(getContainerIP(it), 8529))
                .collect(Collectors.toList());
    }

    @Override
    public ArangoTopology getTopology() {
        return ArangoTopology.CLUSTER;
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStart() {
        return CompletableFuture
                .runAsync(() -> {
                    network = ReusableNetwork.of("cluster");
                    agents.forEach(agent -> agent.withNetwork(network));
                    dbServers.forEach(agent -> agent.withNetwork(network));
                    coordinators.values().forEach(agent -> agent.withNetwork(network));
                })
                .thenCompose(__ -> performActionOnGroup(agents, GenericContainer::start))
                .thenCompose(__ -> CompletableFuture.allOf(
                        performActionOnGroup(dbServers, GenericContainer::start),
                        performActionOnGroup(coordinators.values(), GenericContainer::start)
                ))
                .thenCompose(__ -> verifyClusterAuthentication())
                .thenCompose(authenticationConfigured -> {
                    if (authenticationConfigured) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return configureClusterAuthentication();
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
                performActionOnGroup(agents, GenericContainer::stop),
                performActionOnGroup(dbServers, GenericContainer::stop),
                performActionOnGroup(coordinators.values(), GenericContainer::stop)
        )
                .thenAcceptAsync(__ -> network.close())
                .thenAccept((v) -> log.info("Cluster has been shutdown!"))
                .thenApply((v) -> this);
    }

    private GenericContainer<?> createContainer(String name, int port) {
        GenericContainer<?> c = new GenericContainer<>(getImage())
                .withReuse(isReuse())
                .withEnv("ARANGO_LICENSE_KEY", getLicenseKey())
                .withCopyFileToContainer(MountableFile.forClasspathResource("deployments/jwtSecret"), "/jwtSecret")
                .withExposedPorts(port)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[" + name + "]"))
                .waitingFor(Wait.forLogMessage(".*up and running.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

        // .withNetworkAliases creates also a random alias, which prevents container reuse
        c.setNetworkAliases(Collections.singletonList(name));
        return c;
    }

    private GenericContainer<?> createAgent(int count) {
        String joinParameter = count == 1 ? " " : "--starter.join agent1 ";
        return createContainer("agent" + count, 8531)
                .withCommand(DOCKER_COMMAND + "--cluster.start-dbserver false --cluster.start-coordinator false " + joinParameter);
    }

    private GenericContainer<?> createDbServer(String name) {
        return createContainer(name, 8530)
                .withCommand(DOCKER_COMMAND + "--cluster.start-dbserver true --cluster.start-coordinator false --starter.join agent1");
    }

    private GenericContainer<?> createCoordinator(String name) {
        return createContainer(name, 8529)
                .withCommand(DOCKER_COMMAND + "--cluster.start-dbserver false --cluster.start-coordinator true --starter.join agent1");
    }

    private CompletableFuture<Void> performActionOnGroup(Collection<GenericContainer<?>> group, Consumer<GenericContainer<?>> action) {
        return CompletableFuture.allOf(
                group.stream()
                        .map(it -> CompletableFuture.runAsync(() -> action.accept(it)))
                        .toArray(CompletableFuture[]::new)
        );
    }

}
