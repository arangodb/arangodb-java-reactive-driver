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


import com.arangodb.reactive.connection.*;
import com.arangodb.reactive.entity.model.ClusterEndpointsEntry;
import com.arangodb.reactive.entity.serde.ArangoSerde;
import com.arangodb.reactive.exceptions.NoHostsAvailableException;
import com.arangodb.reactive.exceptions.server.ArangoServerException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

/**
 * @author Michele Rastelli
 */
public class AcquireHostListMockTest {

    private static final HostDescription initialHost = HostDescription.of("initialHost", 8529);
    private static final int CONNECTIONS_PER_HOST = 3;

    private static CommunicationConfig getConfig(ContentType contentType) {
        return CommunicationConfig.builder()
                .addHosts(initialHost)
                .acquireHostList(true)
                .contentType(contentType)
                .connectionsPerHost(CONNECTIONS_PER_HOST)
                .build();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void newHosts(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Arrays.asList(
                        HostDescription.of("host1", 1111),
                        HostDescription.of("host2", 2222)
                );
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(factory.getHosts().toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void sameHost(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Collections.singletonList(initialHost);
            }
        };
        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactly(factory.getHosts().toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void allUnreachableHosts(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Arrays.asList(
                        HostDescription.of("host1", 1111),
                        HostDescription.of("host2", 2222)
                );
            }

            @Override
            protected void stubIsConnected(ArangoConnection connection, HostDescription host) {
                when(connection.isConnected()).thenReturn(Mono.just(false));
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        Throwable thrown = catchThrowable(() -> communication.initialize().block());
        assertThat(Exceptions.unwrap(thrown)).isInstanceOf(NoHostsAvailableException.class);

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void allUnreachableAcquiredHosts(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Arrays.asList(
                        HostDescription.of("host1", 1111),
                        HostDescription.of("host2", 2222)
                );
            }

            @Override
            protected void stubIsConnected(ArangoConnection connection, HostDescription host) {
                if (getHosts().contains(host)) {
                    when(connection.isConnected()).thenReturn(Mono.just(false));
                } else {
                    when(connection.isConnected()).thenReturn(Mono.just(true));
                }
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        Throwable thrown = catchThrowable(() -> communication.initialize().block());
        assertThat(Exceptions.unwrap(thrown)).isInstanceOf(NoHostsAvailableException.class);

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void someUnreachableAcquiredHosts(ContentType contentType) {
        List<HostDescription> unreachableHosts = Collections.singletonList(HostDescription.of("host1", 1111));
        List<HostDescription> reachableHosts = Collections.singletonList(HostDescription.of("host2", 2222));

        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Stream.concat(unreachableHosts.stream(), reachableHosts.stream()).collect(Collectors.toList());
            }

            @Override
            protected void stubIsConnected(ArangoConnection connection, HostDescription host) {
                if (unreachableHosts.contains(host)) {
                    when(connection.isConnected()).thenReturn(Mono.just(false));
                } else {
                    when(connection.isConnected()).thenReturn(Mono.just(true));
                }
            }

            @Override
            public Mono<ArangoConnection> create(HostDescription host, AuthenticationMethod authentication) {
                if (unreachableHosts.contains(host)) {
                    return Mono.error(new IOException("Connection closed!"));
                } else {
                    return super.create(host, authentication);
                }
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactly(reachableHosts.toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void acquireHostListRequestError(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Arrays.asList(
                        HostDescription.of("host1", 1111),
                        HostDescription.of("host2", 2222)
                );
            }

            @Override
            protected void stubRequestClusterEndpoints(ArangoConnection connection) {
                when(connection.execute(any(ArangoRequest.class))).thenReturn(Mono.just(
                        ArangoResponse.builder()
                                .responseCode(500)
                                .body(
                                        ArangoSerde.of(contentType).serialize(MockErrorEntity
                                                .builder()
                                                .errorMessage("Error 8000")
                                                .errorNum(8000)
                                                .code(500)
                                                .error(true)
                                                .build())
                                )
                                .build()
                ));
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        Throwable thrown = catchThrowable(() -> communication.initialize().block());
        assertThat(Exceptions.unwrap(thrown))
                .isInstanceOf(ArangoServerException.class)
                .hasMessageContaining("Error 8000");

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void acquireHostListWithSomeRequestError(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Arrays.asList(
                        HostDescription.of("host1", 1111),
                        HostDescription.of("host2", 2222)
                );
            }

            @Override
            protected void stubRequestClusterEndpoints(ArangoConnection connection) {
                when(connection.execute(any(ArangoRequest.class)))
                        .thenReturn(Mono.just(
                                ArangoResponse.builder()
                                        .responseCode(500)
                                        .body(
                                                ArangoSerde.of(contentType).serialize(MockErrorEntity
                                                        .builder()
                                                        .errorMessage("Error 8000")
                                                        .errorNum(8000)
                                                        .code(500)
                                                        .error(true)
                                                        .build())
                                        )
                                        .build()
                        ))
                        .thenReturn(getClusterEndpointsResponse());
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(factory.getHosts().toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void acquireHostListWithIpV6Response(ContentType contentType) {
        MockConnectionFactory factory = new MockConnectionFactory(contentType) {
            @Override
            List<HostDescription> getHosts() {
                return Arrays.asList(
                        HostDescription.of("0:0:0:0:0:0:0:1", 1111),
                        HostDescription.of("0:0:0:0:0:0:0:2", 2222)
                );
            }
        };

        ArangoCommunicationImpl communication = new ArangoCommunicationImpl(getConfig(contentType), factory);
        communication.initialize().block();

        ConnectionPoolImpl connectionPool = (ConnectionPoolImpl) communication.getConnectionPool();
        assertThat(connectionPool.getConnectionsByHost().keySet()).containsExactlyInAnyOrder(factory.getHosts().toArray(new HostDescription[0]));
        connectionPool.getConnectionsByHost().values().forEach(connections -> assertThat(connections).hasSize(CONNECTIONS_PER_HOST));
    }

    static abstract class MockConnectionFactory implements ConnectionFactory {

        private final ContentType contentType;

        public MockConnectionFactory(ContentType contentType) {
            this.contentType = contentType;
        }

        private static String getHostUrl(HostDescription hostDescription) {
            final boolean isIpv6 = hostDescription.getHost().contains(":");
            String hostUrl = "tcp://";

            if (isIpv6) {
                hostUrl += "[";
            }

            hostUrl += hostDescription.getHost();

            if (isIpv6) {
                hostUrl += "]";
            }

            hostUrl += ":" + hostDescription.getPort();
            return hostUrl;
        }

        abstract List<HostDescription> getHosts();

        protected void stubIsConnected(ArangoConnection connection, HostDescription host) {
            when(connection.isConnected()).thenReturn(Mono.just(true));
        }

        protected Mono<ArangoResponse> getClusterEndpointsResponse() {
            byte[] responseBody = ArangoSerde
                    .of(contentType)
                    .serialize(
                            MockClusterEndpoints.builder()
                                    .error(false)
                                    .code(200)
                                    .endpoints(getHosts().stream()
                                            .map(it -> ClusterEndpointsEntry.of(getHostUrl(it)))
                                            .collect(Collectors.toList()))
                                    .build()
                    );

            return Mono.just(
                    ArangoResponse.builder()
                            .responseCode(200)
                            .body(responseBody)
                            .build()
            );
        }

        protected void stubRequestClusterEndpoints(ArangoConnection connection) {
            when(connection.execute(any(ArangoRequest.class))).thenReturn(getClusterEndpointsResponse());
        }

        @Override
        public Mono<ArangoConnection> create(HostDescription host, AuthenticationMethod authentication) {
            ArangoConnection connection = mock(ArangoConnection.class);
            stubIsConnected(connection, host);
            stubRequestClusterEndpoints(connection);
            when(connection.close()).thenReturn(Mono.empty());
            return Mono.just(connection);
        }

        @Override
        public void close() {
        }

    }

}
