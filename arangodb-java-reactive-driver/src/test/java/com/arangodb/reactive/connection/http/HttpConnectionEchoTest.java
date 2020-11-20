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

package com.arangodb.reactive.connection.http;

import com.arangodb.reactive.connection.*;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.netty.DisposableServer;
import utils.EchoHttpServer;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
class HttpConnectionEchoTest {

    private static DisposableServer server;

    private final HostDescription host = HostDescription.of("localhost", 9000);
    private final AuthenticationMethod authentication = AuthenticationMethod.ofJwt("user", "token");

    private final ConnectionConfig config = ConnectionConfig.builder()
            .contentType(ContentType.JSON)
            .build();

    private final String body = "{\"message\": \"Hello World!\"}";

    private final Map.Entry<String, String> headerParam = MapEntry.entry("header-param-key", "headerParamValue");
    private final Map.Entry<String, Optional<String>> queryParam = MapEntry.entry("query-param-key", Optional.of("queryParamValue"));

    private final ArangoRequest request = ArangoRequest.builder()
            .database("database")
            .path("/path")
            .putHeaderParams(headerParam)
            .putQueryParams(queryParam)
            .requestType(ArangoRequest.RequestType.POST)
            .body(body.getBytes())
            .build();

    @BeforeAll
    static void setup() {
        server = new EchoHttpServer().start().join();
    }

    @AfterAll
    static void shutDown() {
        server.dispose();
        server.onDispose().block();
    }

    @Test
    void execute() {
        HttpConnection connection = new Http11Connection(host, authentication, config);
        ArangoResponse response = connection.execute(request).block();

        // authorization
        assertThat(response).isNotNull();
        assertThat(response.getMeta()).containsKey("authorization");
        assertThat(response.getMeta().get("authorization")).isEqualTo("Bearer token");

        // body
        String receivedString = new String(response.getBody());

        assertThat(receivedString).isEqualTo(body);

        // headers
        assertThat(response.getMeta()).containsKey(headerParam.getKey());
        assertThat(response.getMeta().get(headerParam.getKey())).isEqualTo(headerParam.getValue());

        // accept header
        assertThat(response.getMeta()).containsKey("accept");
        assertThat(response.getMeta().get("accept")).isEqualTo("application/json");

        // uri & params
        assertThat(response.getMeta()).containsKey("uri");
        assertThat(response.getMeta().get("uri"))
                .isEqualTo("/_db/database/path?" + queryParam.getKey() + "=" + queryParam.getValue().get());

        // responseCode
        assertThat(response.getResponseCode()).isEqualTo(200);
    }

    @Test
    void executeVPack() {
        HttpConnection connection = new Http11Connection(host, authentication, ConnectionConfig.builder().from(config)
                .contentType(ContentType.VPACK)
                .build());

        final VPackBuilder builder = new VPackBuilder();
        builder.add(ValueType.OBJECT);
        builder.add("message", "Hello World!");
        builder.close();
        final VPackSlice slice = builder.slice();

        ArangoResponse response = connection.execute(ArangoRequest.builder().from(request)
                .body(slice.getBuffer())
                .build()).block();

        // body
        assertThat(response).isNotNull();
        VPackSlice receivedSlice = new VPackSlice(response.getBody());

        assertThat(receivedSlice).isEqualTo(slice);
        assertThat(receivedSlice.get("message").getAsString()).isEqualTo("Hello World!");

        // accept header
        assertThat(response.getMeta()).containsKey("accept");
        assertThat(response.getMeta().get("accept")).isEqualTo("application/x-velocypack");
    }

    @Test
    void executeEmptyBody() {
        HttpConnection connection = new Http11Connection(host, AuthenticationMethod.ofBasic("user", "password"), config);

        ArangoResponse response = connection.execute(ArangoRequest.builder().from(request).body().build()).block();

        // body
        assertThat(response).isNotNull();
        assertThat(response.getBody().length).isEqualTo(0);
    }

    @Test
    void executeBasicAuthentication() {
        HttpConnection connection = new Http11Connection(host, AuthenticationMethod.ofBasic("user", "password"), config);

        ArangoResponse response = connection.execute(request).block();

        // authorization
        assertThat(response).isNotNull();
        assertThat(response.getMeta()).containsKey("authorization");
        assertThat(response.getMeta().get("authorization")).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
    }

}