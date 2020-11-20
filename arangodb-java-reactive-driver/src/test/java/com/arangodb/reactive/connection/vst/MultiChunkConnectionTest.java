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

package com.arangodb.reactive.connection.vst;

import com.arangodb.reactive.connection.*;
import deployments.ContainerDeployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.arangodb.reactive.connection.ConnectionTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
@Testcontainers
class MultiChunkConnectionTest {

    private static final int CHUNK_SIZE = 1_000;
    @Container
    private static final ContainerDeployment deployment = ContainerDeployment.ofSingleServerNoAuth();
    private static HostDescription host;
    private final ConnectionConfig config;

    MultiChunkConnectionTest() {
        config = ConnectionConfig.builder()
                .chunkSize(CHUNK_SIZE)
                .build();
    }

    @BeforeAll
    static void setup() {
        host = deployment.getHosts().get(0);
    }

    @Test
    void getRequest() {
        ArangoConnection connection = new ConnectionFactoryImpl(config, ArangoProtocol.VST, DEFAULT_SCHEDULER_FACTORY)
                .create(host, null).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.VERSION_REQUEST).block();
        verifyGetResponseVPack(response);
        connection.close().block();
    }

    @Test
    void postRequest() {
        ArangoConnection connection = new ConnectionFactoryImpl(config, ArangoProtocol.VST, DEFAULT_SCHEDULER_FACTORY)
                .create(host, null).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.postRequest()).block();
        verifyPostResponseVPack(response);
        connection.close().block();
    }

    @Test
    void bigRequest() {
        ArangoConnection connection = new ConnectionFactoryImpl(config, ArangoProtocol.VST, DEFAULT_SCHEDULER_FACTORY)
                .create(host, null).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.bigRequest()).block();
        verifyBigAqlQueryResponseVPack(response);
        connection.close().block();
    }

}