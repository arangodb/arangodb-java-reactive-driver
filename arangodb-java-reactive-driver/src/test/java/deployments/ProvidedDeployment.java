package deployments;


import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.connection.AuthenticationMethod;
import com.arangodb.reactive.connection.HostDescription;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProvidedDeployment extends ContainerDeployment {

    private final List<HostDescription> hosts;
    private final ArangoTopology topology;
    private final AuthenticationMethod authentication;

    ProvidedDeployment(List<HostDescription> hosts, ArangoTopology topology, AuthenticationMethod authentication) {
        this.hosts = hosts;
        this.topology = topology;
        this.authentication = authentication;
    }

    @Override
    public List<HostDescription> getHosts() {
        return hosts;
    }

    @Override
    public ArangoTopology getTopology() {
        return topology;
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStart() {
        return CompletableFuture.completedFuture(this);
    }

    @Override
    CompletableFuture<ContainerDeployment> asyncStop() {
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public AuthenticationMethod getAuthentication() {
        return authentication;
    }

    @Override
    public String getUser() {
        throw new IllegalStateException("This is operation is not provided for ProvidedDeployment");
    }

    @Override
    public String getPassword() {
        throw new IllegalStateException("This is operation is not provided for ProvidedDeployment");
    }

    @Override
    public String getBasicAuthentication() {
        throw new IllegalStateException("This is operation is not provided for ProvidedDeployment");
    }

    @Override
    protected String getSslProtocol() {
        throw new IllegalStateException("This is operation is not provided for ProvidedDeployment");
    }
}
