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
import com.arangodb.reactive.connection.ConnectionConfig;
import com.arangodb.reactive.connection.ConnectionFactoryImpl;
import com.arangodb.reactive.connection.ConnectionSchedulerFactory;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
public interface ArangoCommunication {

    /**
     * Reactor Context key identifying the conversation
     */
    String CONVERSATION_CTX = "conversation";

    static Mono<ArangoCommunication> create(CommunicationConfig config) {
        ConnectionConfig connectionConfig = ConnectionConfig.builder().from(config.getConnectionConfig())
                // override connection content type
                .contentType(config.getContentType())
                .build();

        ConnectionFactoryImpl connectionFactory = new ConnectionFactoryImpl(
                connectionConfig,
                config.getProtocol(),
                new ConnectionSchedulerFactory(config.getMaxThreads())
        );
        return new ArangoCommunicationImpl(config, connectionFactory).initialize();
    }

    /**
     * Initializes the communication asynchronously performing the following tasks:
     * - negotiate authentication (eg. Kerberos),
     * - acquire the host list from server,
     * - create and initialize the connections
     *
     * @return the communication ready to be used
     */
    Mono<ArangoCommunication> initialize();

    /**
     * Send the request to a random host over a random connection.
     * In case the request should be sent to a specific host, a {@link Conversation} can be specified setting the
     * {@link ArangoCommunication#CONVERSATION_CTX} field in the subscriberContext.
     * Such conversation will be shared with all the upstream steps.
     * <p>
     * In the following example {@code request1} and {@code request2} will be executed within the same conversation:
     *
     * <pre>
     * {@code
     * arangoCommunication.execute(request1)
     *     .then(arangoCommunication.execute(request2))
     *     .subscriberContext(ctx ->
     *         ctx.put(ArangoCommunication.CONVERSATION_CTX, arangoCommunication.createConversation(Conversation.Level.REQUIRED)));
     * }
     * </pre>
     *
     * @param request to send
     * @return response from the server
     * @see <a href="https://projectreactor.io/docs/core/release/reference/#context.api">Reactor Context</a>
     */
    Mono<ArangoResponse> execute(ArangoRequest request);

    /**
     * @return a new conversation
     */
    Conversation createConversation(Conversation.Level level);

    /**
     * @return a mono completing once all the connections are closed
     */
    Mono<Void> close();

}
