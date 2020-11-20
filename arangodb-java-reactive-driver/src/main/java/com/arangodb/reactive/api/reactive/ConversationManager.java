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

package com.arangodb.reactive.api.reactive;


import com.arangodb.reactive.communication.Conversation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
public interface ConversationManager {

    /**
     * Creates a new {@link Conversation} delegating {@link com.arangodb.reactive.communication.ArangoCommunication#createConversation(Conversation.Level)}
     *
     * @return a new conversation
     */
    Conversation createConversation(Conversation.Level level);

    /**
     * Creates a new conversation and binds {@code publisher} to it. All the requests performed by {@code publisher}
     * will be executed against the same coordinator. In case this is not possible it will behave according to the
     * specified conversation level {@link Conversation.Level}. Eg.:
     *
     * <pre>
     * {@code
     *         Mono<DatabaseEntity> db = arangoDB.requireConversation(
     *                 arangoDB
     *                         .createDatabase(name)
     *                         .then(arangoDB.getDatabase(name))
     *         );
     * }
     * </pre>
     *
     * @param publisher a {@link org.reactivestreams.Publisher} performing many db requests
     * @return a contextualized {@link org.reactivestreams.Publisher} with configured context conversation
     */
    <T> Mono<T> requireConversation(Mono<T> publisher);

    /**
     * Creates a new conversation and binds {@code publisher} to it. All the requests performed by {@code publisher}
     * will be executed against the same coordinator. In case this is not possible it will behave according to the
     * specified conversation level {@link Conversation.Level}. Eg.:
     *
     * <pre>
     * {@code
     *         Mono<DatabaseEntity> db = arangoDB.requireConversation(
     *                 arangoDB
     *                         .createDatabase(name)
     *                         .then(arangoDB.getDatabase(name))
     *         );
     * }
     * </pre>
     *
     * @param publisher a {@link org.reactivestreams.Publisher} performing many db requests
     * @return a contextualized {@link org.reactivestreams.Publisher} with configured context conversation
     */
    <T> Flux<T> requireConversation(Flux<T> publisher);

    /**
     * Creates a new conversation and binds {@code publisher} to it. All the requests performed by {@code publisher}
     * will be executed against the same coordinator. In case this is not possible it will behave according to the
     * specified conversation level {@link Conversation.Level}. Eg.:
     *
     * <pre>
     * {@code
     *         Mono<DatabaseEntity> db = arangoDB.preferConversation(
     *                 arangoDB
     *                         .createDatabase(name)
     *                         .then(arangoDB.getDatabase(name))
     *         );
     * }
     * </pre>
     *
     * @param publisher a {@link org.reactivestreams.Publisher} performing many db requests
     * @return a contextualized {@link org.reactivestreams.Publisher} with configured context conversation
     */
    <T> Mono<T> preferConversation(Mono<T> publisher);

    /**
     * Creates a new conversation and binds {@code publisher} to it. All the requests performed by {@code publisher}
     * will be executed against the same coordinator. In case this is not possible it will behave according to the
     * specified conversation level {@link Conversation.Level}. Eg.:
     *
     * <pre>
     * {@code
     *         Mono<DatabaseEntity> db = arangoDB.preferConversation(
     *                 arangoDB
     *                         .createDatabase(name)
     *                         .then(arangoDB.getDatabase(name))
     *         );
     * }
     * </pre>
     *
     * @param publisher a {@link org.reactivestreams.Publisher} performing many db requests
     * @return a contextualized {@link org.reactivestreams.Publisher} with configured context conversation
     */
    <T> Flux<T> preferConversation(Flux<T> publisher);

    /**
     * Executes {@code publisher} within {@code conversation}
     *
     * @param conversation to use
     * @param publisher    to be executed
     * @return a contextualized {@link org.reactivestreams.Publisher} with configured context conversation
     */
    <T> Mono<T> useConversation(Conversation conversation, Mono<T> publisher);

    /**
     * Executes {@code publisher} within {@code conversation}
     *
     * @param conversation to use
     * @param publisher    to be executed
     * @return a contextualized {@link org.reactivestreams.Publisher} with configured context conversation
     */
    <T> Flux<T> useConversation(Conversation conversation, Flux<T> publisher);

}
