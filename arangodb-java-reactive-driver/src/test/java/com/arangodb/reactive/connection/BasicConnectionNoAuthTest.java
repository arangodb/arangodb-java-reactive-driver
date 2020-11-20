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

package com.arangodb.reactive.connection;

import deployments.ContainerDeployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.Exceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.arangodb.reactive.connection.ConnectionTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
@Testcontainers
class BasicConnectionNoAuthTest {

    @Container
    private static final ContainerDeployment deployment = ContainerDeployment.ofSingleServerNoAuth();
    private static HostDescription host;
    private final ConnectionConfig config;

    BasicConnectionNoAuthTest() {
        config = ConnectionConfig.builder()
                .build();
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

    @BeforeAll
    static void setup() {
        host = deployment.getHosts().get(0);
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void getRequest(ArangoProtocol protocol) {
        ArangoConnection connection = new ConnectionFactoryImpl(config, protocol, DEFAULT_SCHEDULER_FACTORY)
                .create(host, null).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.VERSION_REQUEST).block();
        verifyGetResponseVPack(response);
        connection.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void postRequest(ArangoProtocol protocol) {
        ArangoConnection connection = new ConnectionFactoryImpl(config, protocol, DEFAULT_SCHEDULER_FACTORY)
                .create(host, null).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.postRequest()).block();
        verifyPostResponseVPack(response);
        connection.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void parallelLoop(ArangoProtocol protocol) {
        new ConnectionFactoryImpl(config, protocol, DEFAULT_SCHEDULER_FACTORY)
                .create(host, deployment.getAuthentication())
                .flatMap(c -> c
                        .execute(ConnectionTestUtils.VERSION_REQUEST)
                        .doOnNext(ConnectionTestUtils::verifyGetResponseVPack)
                        .repeat(1_000)
                        .then()
                )
                .then().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void wrongHostFailure(ArangoProtocol protocol) {
        HostDescription wrongHost = HostDescription.of("wrongHost", 8529);
        Throwable thrown = catchThrowable(() -> new ConnectionFactoryImpl(config, protocol, DEFAULT_SCHEDULER_FACTORY).create(wrongHost, null)
                .block());
        assertThat(Exceptions.unwrap(thrown)).isInstanceOf(IOException.class);
    }

}