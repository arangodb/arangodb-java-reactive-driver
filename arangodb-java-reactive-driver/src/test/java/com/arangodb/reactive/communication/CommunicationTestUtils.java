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

import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import com.arangodb.reactive.connection.ConnectionTestUtils;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.velocypack.VPackSlice;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.arangodb.reactive.connection.ConnectionTestUtils.verifyGetResponseVPack;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
class CommunicationTestUtils {

    private static final ArangoRequest STATUS_REQUEST = ArangoRequest.builder()
            .database("_system")
            .path("/_admin/status")
            .requestType(ArangoRequest.RequestType.GET)
            .build();

    static void executeRequest(ArangoCommunication communication, int retries) {
        ArangoResponse response = communication.execute(ConnectionTestUtils.VERSION_REQUEST)
                .retryWhen(Retry.max(retries).filter(t -> t instanceof IOException || t instanceof TimeoutException))
                .block();
        verifyGetResponseVPack(response);
    }

    static void executeRequest(ArangoCommunication communication) {
        ArangoResponse response = communication.execute(ConnectionTestUtils.VERSION_REQUEST).block();
        verifyGetResponseVPack(response);
    }

    static void executeRequestAndVerifyHost(ArangoCommunication communication, Conversation conversation, boolean expectSameHost) {
        String remoteHost = executeStatusRequest(communication, conversation);
        if (expectSameHost) {
            HostDescription host = conversation.getHost();
            assertThat(remoteHost).isEqualTo(host.getHost());
        }
    }

    static String executeStatusRequest(ArangoCommunication communication, Conversation conversation) {
        ArangoResponse response = communication.execute(STATUS_REQUEST)
                .contextWrite(ctx -> ctx.put(ArangoCommunication.CONVERSATION_CTX, conversation))
                .block();
        return getHostFromStatusResponseVPack(response);
    }

    static String executeStatusRequest(ArangoCommunication communication) {
        ArangoResponse response = communication.execute(STATUS_REQUEST).block();
        return getHostFromStatusResponseVPack(response);
    }

    private static String getHostFromStatusResponseVPack(ArangoResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getType()).isEqualTo(2);
        assertThat(response.getResponseCode()).isEqualTo(200);

        VPackSlice slice = new VPackSlice(response.getBody());
        return slice.get("host").getAsString();
    }

}
