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


import com.arangodb.reactive.entity.model.Version;
import com.arangodb.reactive.entity.serde.ArangoSerde;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
public class ConnectionTestUtils {
    public static final ConnectionSchedulerFactory DEFAULT_SCHEDULER_FACTORY = new ConnectionSchedulerFactory(4);
    public static final ArangoRequest VERSION_REQUEST = ArangoRequest.builder()
            .database("_system")
            .path("/_api/version")
            .requestType(ArangoRequest.RequestType.GET)
            .putQueryParams("details", Optional.of("true"))
            .build();
    private static final String AQL_QUERY_VALUE = new String(new char[1_000_000]).replace('\0', 'x');

    public static ArangoRequest postRequest() {
        return ArangoRequest.builder()
                .database("_system")
                .path("/_api/query")
                .requestType(ArangoRequest.RequestType.POST)
                .body(createParseQueryRequestBody().toByteArray())
                .build();
    }

    public static ArangoRequest bigRequest() {
        return ArangoRequest.builder()
                .database("_system")
                .path("/_api/cursor")
                .requestType(ArangoRequest.RequestType.POST)
                .body(createBigAqlQueryRequestBody().toByteArray())
                .build();
    }

    public static void performRequest(ArangoConnection connection) {
        ArangoResponse response = connection.execute(VERSION_REQUEST).block();
        verifyGetResponseVPack(response);
    }

    public static void verifyGetResponseVPack(ArangoResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getType()).isEqualTo(2);
        assertThat(response.getResponseCode()).isEqualTo(200);

        ArangoSerde serde = ArangoSerde.of(ContentType.VPACK);
        Version version = serde.deserialize(response.getBody(), Version.class);
        assertThat(version.getServer()).isEqualTo("arango");
    }

    public static void verifyPostResponseVPack(ArangoResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getType()).isEqualTo(2);
        assertThat(response.getResponseCode()).isEqualTo(200);

        VPackSlice responseBodySlice = new VPackSlice(response.getBody());
        assertThat(responseBodySlice.get("parsed").getAsBoolean()).isEqualTo(true);
    }

    private static VPackSlice createParseQueryRequestBody() {
        final VPackBuilder builder = new VPackBuilder();
        builder.add(ValueType.OBJECT);
        builder.add("query", "FOR i IN 1..100 RETURN i * 3");
        builder.close();
        return builder.slice();
    }

    private static VPackSlice createBigAqlQueryRequestBody() {
        final VPackBuilder builder = new VPackBuilder();
        builder.add(ValueType.OBJECT);
        builder.add("query", "RETURN @value");
        builder.add("bindVars", ValueType.OBJECT);
        builder.add("value", AQL_QUERY_VALUE);
        builder.close();
        builder.close();
        return builder.slice();
    }

    public static void verifyBigAqlQueryResponseVPack(ArangoResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getType()).isEqualTo(2);
        assertThat(response.getResponseCode()).isEqualTo(201);

        VPackSlice responseBodySlice = new VPackSlice(response.getBody());
        assertThat(responseBodySlice.get("result").arrayIterator().next().getAsString()).isEqualTo(AQL_QUERY_VALUE);
    }

}
