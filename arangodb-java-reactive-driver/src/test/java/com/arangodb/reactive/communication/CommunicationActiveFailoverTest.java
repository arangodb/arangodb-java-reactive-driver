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

import com.arangodb.reactive.connection.ArangoConnection;
import com.arangodb.reactive.connection.ArangoProtocol;
import com.arangodb.reactive.connection.ConnectionTestUtils;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.reactive.exceptions.NoHostsAvailableException;
import deployments.ContainerDeployment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.Exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
@Testcontainers
class CommunicationActiveFailoverTest {

    @Container
    private final static ContainerDeployment deployment = ContainerDeployment.ofReusableActiveFailover();
    private final CommunicationConfigBuilder config;
    private final List<HostDescription> hosts;

    CommunicationActiveFailoverTest() {
        hosts = deployment.getHosts();
        config = CommunicationConfig.builder()
                .topology(ArangoTopology.ACTIVE_FAILOVER)
                .addAllHosts(hosts)
                .acquireHostList(true)
                .authenticationMethod(deployment.getAuthentication());
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

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void create(ArangoProtocol protocol) {
        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .build()).block();
        assertThat(communication).isNotNull();

        ArangoCommunicationImpl communicationImpl = ((ArangoCommunicationImpl) communication);
        ActiveFailoverConnectionPool connectionPool = (ActiveFailoverConnectionPool) communicationImpl.getConnectionPool();
        Map<HostDescription, List<ArangoConnection>> connectionsByHost = connectionPool.getConnectionsByHost();
        HostDescription[] expectedKeys = hosts.toArray(new HostDescription[0]);
        assertThat(connectionsByHost)
                .containsKeys(expectedKeys)
                .containsOnlyKeys(expectedKeys);

        HostDescription leader = connectionPool.getLeader();
        assertThat(hosts).contains(leader);

        // check if every connection is connected
        connectionsByHost.forEach((key, value) -> value.forEach(connection -> {
            assertThat(connection.isConnected().block()).isTrue();
            ConnectionTestUtils.performRequest(connection);
        }));

        CommunicationTestUtils.executeRequest(communication);
        communication.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void executeWithConversation(ArangoProtocol protocol) {
        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .build()).block();
        assertThat(communication).isNotNull();

        Conversation requiredConversation = communication.createConversation(Conversation.Level.REQUIRED);
        for (int i = 0; i < 10; i++) {
            CommunicationTestUtils.executeRequestAndVerifyHost(communication, requiredConversation, true);
        }

        Conversation preferredConversation = communication.createConversation(Conversation.Level.PREFERRED);
        for (int i = 0; i < 10; i++) {
            CommunicationTestUtils.executeRequestAndVerifyHost(communication, preferredConversation, true);
        }

        communication.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void dirtyRead(ArangoProtocol protocol) {
        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .dirtyReads(true)
                .build()).block();
        assertThat(communication).isNotNull();

        Set<String> remoteHosts = new HashSet<>();

        for (int i = 0; i < 30; i++) {
            String host = CommunicationTestUtils.executeStatusRequest(communication);
            remoteHosts.add(host);
        }

        assertThat(remoteHosts)
                .containsExactlyInAnyOrderElementsOf(
                        hosts
                                .stream()
                                .map(HostDescription::getHost)
                                .collect(Collectors.toList()));

        communication.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void executeOnLeader(ArangoProtocol protocol) {
        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .build()).block();
        assertThat(communication).isNotNull();

        Set<String> remoteHosts = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            String host = CommunicationTestUtils.executeStatusRequest(communication);
            remoteHosts.add(host);
        }

        assertThat(remoteHosts).hasSize(1);
        communication.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void acquireHostList(ArangoProtocol protocol) {

        ArangoCommunication communication = ArangoCommunication.create(config
                .protocol(protocol)
                .hosts(hosts.subList(0, 1))
                .build()).block();
        assertThat(communication).isNotNull();

        ArangoCommunicationImpl communicationImpl = ((ArangoCommunicationImpl) communication);
        ActiveFailoverConnectionPool connectionPool = (ActiveFailoverConnectionPool) communicationImpl.getConnectionPool();
        Map<HostDescription, List<ArangoConnection>> currentHosts = connectionPool.getConnectionsByHost();
        assertThat(currentHosts).hasSize(hosts.size());
        assertThat(currentHosts.keySet()).containsExactlyInAnyOrderElementsOf(hosts);

        HostDescription leader = connectionPool.getLeader();
        assertThat(hosts).contains(leader);

        // check if every connection is connected
        currentHosts.forEach((key, value) -> value.forEach(connection -> {
            assertThat(connection.isConnected().block()).isTrue();
        }));

        CommunicationTestUtils.executeRequest(communication);
        communication.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void wrongHostConnectionFailure(ArangoProtocol protocol) {
        Throwable thrown = catchThrowable(() -> ArangoCommunication.create(config
                .protocol(protocol)
                .hosts(Collections.singleton(HostDescription.of("wrongHost", 8529)))
                .retries(0)
                .build()).block());
        assertThat(Exceptions.unwrap(thrown)).isInstanceOf(NoHostsAvailableException.class);
    }

}
