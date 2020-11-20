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


package com.arangodb.reactive.communication;

import com.arangodb.reactive.connection.ArangoProtocol;
import com.arangodb.reactive.connection.HostDescription;
import deployments.ProxiedContainerDeployment;
import deployments.ProxiedHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.Exceptions;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.arangodb.reactive.communication.CommunicationTestUtils.executeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
@Tag("resiliency")
@Testcontainers
class ActiveFailoverResiliencyTest {

    @Container
    private final static ProxiedContainerDeployment deployment = ProxiedContainerDeployment.ofActiveFailover(3);
    private final CommunicationConfigBuilder config;

    ActiveFailoverResiliencyTest() {
        config = CommunicationConfig.builder()
                .topology(ArangoTopology.ACTIVE_FAILOVER)
                .addAllHosts(deployment.getHosts())
                .authenticationMethod(deployment.getAuthentication())
                // use proxied hostDescriptions
                .acquireHostList(false);
    }

    static private Stream<Arguments> argumentsProvider() {
        List<ArangoProtocol> protocols = new ArrayList<>();
        protocols.add(ArangoProtocol.VST);
        protocols.add(ArangoProtocol.HTTP11);

        if (deployment.isAtLeastVersion(3, 7)) {
            protocols.add(ArangoProtocol.HTTP2);
        }

        return protocols.stream().map(Arguments::arguments);
    }

    @BeforeEach
    void restore() {
        deployment.getProxiedHosts().forEach(it -> {
            it.enableProxy();
            it.getProxy().setConnectionCut(false);
        });
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void retry(ArangoProtocol protocol) {
        CommunicationConfig testConfig = config
                .protocol(protocol)
                .timeout(Duration.ofMillis(1000))
                .build();
        ArangoCommunication communication = ArangoCommunication.create(testConfig).block();
        assertThat(communication).isNotNull();

        List<ProxiedHost> proxies = deployment.getProxiedHosts();

        Map<HostDescription, ProxiedHost> proxiedHosts = proxies.stream()
                .map(it -> new AbstractMap.SimpleEntry<>(it.getHostDescription(), it))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        HostDescription leader = ((ActiveFailoverConnectionPool) ((ArangoCommunicationImpl) communication)
                .getConnectionPool()).getLeader();

        ProxiedHost leaderProxy = proxiedHosts.get(leader);

        // FIXME: cycle more times once HTTP2 reconnection works faster
        for (int j = 0; j < 2; j++) {
            System.out.println(j);
            executeRequest(communication, 2);
            leaderProxy.disableProxy();

            assertThat(Exceptions.unwrap(catchThrowable(() -> executeRequest(communication, 2))))
                    .isInstanceOf(IllegalStateException.class);
            leaderProxy.enableProxy();
        }

        communication.close().block();
    }

}
