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
import com.arangodb.reactive.connection.ConnectionConfig;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.reactive.exceptions.HostNotAvailableException;
import com.arangodb.reactive.exceptions.NoHostsAvailableException;
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

import java.io.IOException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.arangodb.reactive.communication.CommunicationTestUtils.executeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Michele Rastelli
 */
@Tag("resiliency")
@Testcontainers
class CommunicationResiliencyTest {

    @Container
    private final static ProxiedContainerDeployment deployment = ProxiedContainerDeployment.ofCluster(2, 2);
    private final CommunicationConfigBuilder config;

    CommunicationResiliencyTest() {
        config = CommunicationConfig.builder()
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
        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .connectionConfig(
                        ConnectionConfig.builder()
                                .timeout(Duration.ofSeconds(1))
                                .build())
                .build()).block();
        assertThat(communication).isNotNull();

        ProxiedHost host0 = deployment.getProxiedHosts().get(0);
        ProxiedHost host1 = deployment.getProxiedHosts().get(1);

        // FIXME: cycle more times once HTTP2 reconnection works faster
        for (int j = 0; j < 5; j++) {
            executeRequest(communication, 2); // retries at most once per host
            host0.disableProxy();
            executeRequest(communication, 100);
            host1.disableProxy();

            Throwable thrown = catchThrowable(() -> executeRequest(communication));
            assertThat(Exceptions.unwrap(thrown)).isInstanceOfAny(IOException.class, TimeoutException.class);

            host0.enableProxy();
            host1.enableProxy();
        }

        communication.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void executeWithConversation(ArangoProtocol protocol) {
        // FIXME: re-enable when HTTP2 will detect disconnections before timeout
        assumeTrue(!protocol.equals(ArangoProtocol.HTTP2));

        List<ProxiedHost> proxies = Arrays.asList(
                deployment.getProxiedHosts().get(0),
                deployment.getProxiedHosts().get(1)
        );

        Map<HostDescription, ProxiedHost> proxiedHosts = proxies.stream()
                .map(it -> new AbstractMap.SimpleEntry<>(it.getHostDescription(), it))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .timeout(Duration.ofSeconds(1))
                .build()).block();
        assertThat(communication).isNotNull();

        for (int i = 0; i < 10; i++) {
            Conversation requiredConversation = communication.createConversation(Conversation.Level.REQUIRED);

            // update connections removing conversation host
            proxiedHosts.get(requiredConversation.getHost()).disableProxy();
            ((ArangoCommunicationImpl) communication).getConnectionPool().updateConnections(proxiedHosts.keySet()).block();

            assertThat(catchThrowable(() ->
                    CommunicationTestUtils.executeRequestAndVerifyHost(communication, requiredConversation, true)))
                    .isInstanceOf(HostNotAvailableException.class);

            Conversation preferredConversation = communication.createConversation(Conversation.Level.PREFERRED);
            CommunicationTestUtils.executeRequestAndVerifyHost(communication, preferredConversation, false);

            // update connections removing all hosts
            proxies.forEach(ProxiedHost::disableProxy);
            ((ArangoCommunicationImpl) communication).getConnectionPool().updateConnections(proxiedHosts.keySet()).block();

            assertThat(catchThrowable(() ->
                    CommunicationTestUtils.executeRequestAndVerifyHost(communication, requiredConversation, true)))
                    .isInstanceOf(HostNotAvailableException.class);

            assertThat(catchThrowable(() ->
                    CommunicationTestUtils.executeRequestAndVerifyHost(communication, preferredConversation, false)))
                    .isInstanceOf(NoHostsAvailableException.class);

            proxies.forEach(ProxiedHost::enableProxy);
            ((ArangoCommunicationImpl) communication).getConnectionPool().updateConnections(proxiedHosts.keySet()).block();
        }

        communication.close().block();
    }

}
