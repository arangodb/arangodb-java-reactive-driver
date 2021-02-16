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
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import com.arangodb.reactive.connection.AuthenticationMethod;
import com.arangodb.reactive.connection.ConnectionFactory;
import com.arangodb.reactive.connection.ContentType;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.reactive.entity.model.ErrorEntity;
import com.arangodb.reactive.entity.serde.ArangoSerde;
import com.arangodb.reactive.exceptions.LeaderNotAvailableException;
import com.arangodb.reactive.exceptions.server.ArangoServerException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static com.arangodb.reactive.connection.ConnectionTestUtils.VERSION_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michele Rastelli
 */
public class ActiveFailoverCommunicationMockTest {

    private static final List<HostDescription> hosts = Arrays.asList(
            HostDescription.of("host0", 8529),
            HostDescription.of("host1", 8529),
            HostDescription.of("host2", 8529)
    );
    private static final int CONNECTIONS_PER_HOST = 3;

    private static CommunicationConfig getConfig(ContentType contentType) {
        return CommunicationConfig.builder()
                .topology(ArangoTopology.ACTIVE_FAILOVER)
                .addAllHosts(hosts)
                .acquireHostList(false)
                .contentType(contentType)
                .connectionsPerHost(CONNECTIONS_PER_HOST)
                .build();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void findLeader(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory() {
            @Override
            protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
                if (host.equals(hosts.get(2))) {
                    when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()));
                } else {
                    when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()));
                }
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ActiveFailoverConnectionPool connectionPool = (ActiveFailoverConnectionPool) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(hosts.toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
        assertThat(connectionPool.getLeader()).isEqualTo(hosts.get(2));
        communication.execute(VERSION_REQUEST).block();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void findLeaderWithTcpError(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory() {
            @Override
            protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
                if (host.equals(hosts.get(2))) {
                    when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()));
                } else {
                    when(connection.requestUser()).thenReturn(Mono.error(new RuntimeException("disconnected!")));
                }
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ActiveFailoverConnectionPool connectionPool = (ActiveFailoverConnectionPool) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(hosts.toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
        assertThat(connectionPool.getLeader()).isEqualTo(hosts.get(2));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void cannotFindLeaderBecauseOf503(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory() {
            @Override
            protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
                when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()));
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        Throwable thrown = catchThrowable(() -> communication.initialize().block());
        assertThat(thrown).isInstanceOf(LeaderNotAvailableException.class);
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void cannotFindLeaderBecauseOfTcpError(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory() {
            @Override
            protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
                when(connection.requestUser()).thenReturn(Mono.error(new RuntimeException("disconnected!")));
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        Throwable thrown = catchThrowable(() -> communication.initialize().block());
        assertThat(thrown).isInstanceOf(LeaderNotAvailableException.class);
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void executeThrowing503ShouldTriggerFindLeader(ContentType contentType) {
        ErrorEntity error = MockErrorEntity.builder()
                .error(true)
                .errorMessage("not a leader")
                .errorNum(1496)
                .code(503)
                .build();

        MockConnectionFactory factory = new MockConnectionFactory() {
            @Override
            protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
                if (host.equals(hosts.get(0))) {
                    when(connection.requestUser())
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()));
                } else if (host.equals(hosts.get(1))) {
                    when(connection.requestUser())
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()));
                } else {
                    when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()));
                }
            }

            @Override
            protected void stubExecute(ArangoConnection connection, HostDescription host) {
                when(connection.execute(any(ArangoRequest.class))).thenReturn(Mono.just(
                        ArangoResponse.builder()
                                .responseCode(503)
                                .body(ArangoSerde.of(contentType).serialize(error))
                                .build()
                ));
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ActiveFailoverConnectionPool connectionPool = (ActiveFailoverConnectionPool) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(hosts.toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
        assertThat(connectionPool.getLeader()).isEqualTo(hosts.get(0));

        Throwable thrown = catchThrowable(() -> communication.execute(VERSION_REQUEST).block());
        assertThat(thrown).isInstanceOf(ArangoServerException.class);
        ArangoServerException e = (ArangoServerException) thrown;
        assertThat(e.getResponseCode()).isEqualTo(503);
        assertThat(e.getEntity().get()).usingRecursiveComparison().isEqualTo(error);

        assertThat(connectionPool.getLeader()).isEqualTo(hosts.get(1));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void executeThrowingTcpErrorShouldTriggerFindLeader(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory() {

            @Override
            protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
                if (host.equals(hosts.get(0))) {
                    when(connection.requestUser())
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()));
                } else if (host.equals(hosts.get(1))) {
                    when(connection.requestUser())
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()))
                            .thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()));
                } else {
                    when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(503).build()));
                }
            }

            @Override
            protected void stubExecute(ArangoConnection connection, HostDescription host) {
                when(connection.execute(any(ArangoRequest.class))).thenReturn(Mono.error(new RuntimeException("disconnected!")));
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ActiveFailoverConnectionPool connectionPool = (ActiveFailoverConnectionPool) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(hosts.toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
        assertThat(connectionPool.getLeader()).isEqualTo(hosts.get(0));
        Throwable thrown = catchThrowable(() -> communication.execute(VERSION_REQUEST).block());
        assertThat(thrown).hasMessage("disconnected!");
        assertThat(connectionPool.getLeader()).isEqualTo(hosts.get(1));
    }

    static abstract class MockConnectionFactory implements ConnectionFactory {

        protected void stubIsConnected(ArangoConnection connection, HostDescription host) {
            when(connection.isConnected()).thenReturn(Mono.just(true));
        }

        protected void stubUserRequest(ArangoConnection connection, HostDescription host) {
            when(connection.requestUser()).thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()));
        }

        protected void stubExecute(ArangoConnection connection, HostDescription host) {
            when(connection.execute(any(ArangoRequest.class))).thenReturn(Mono.just(ArangoResponse.builder().responseCode(200).build()));
        }

        @Override
        public Mono<ArangoConnection> create(HostDescription host, AuthenticationMethod authentication) {
            ArangoConnection connection = mock(ArangoConnection.class);
            stubIsConnected(connection, host);
            stubUserRequest(connection, host);
            stubExecute(connection, host);
            when(connection.close()).thenReturn(Mono.empty());
            return Mono.just(connection);
        }

        @Override
        public void close() {
        }

    }

}
