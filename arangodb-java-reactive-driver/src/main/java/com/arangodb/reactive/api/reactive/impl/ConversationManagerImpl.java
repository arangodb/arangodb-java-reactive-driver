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


package com.arangodb.reactive.api.reactive.impl;

import com.arangodb.reactive.api.reactive.ConversationManager;
import com.arangodb.reactive.communication.ArangoCommunication;
import com.arangodb.reactive.communication.Conversation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
public final class ConversationManagerImpl implements ConversationManager {

    private final ArangoCommunication communication;

    public ConversationManagerImpl(final ArangoCommunication arangoCommunication) {
        this.communication = arangoCommunication;
    }

    @Override
    public Conversation createConversation(final Conversation.Level level) {
        return communication.createConversation(level);
    }

    @Override
    public <T> Mono<T> requireConversation(final Mono<T> publisher) {
        return useConversation(createConversation(Conversation.Level.REQUIRED), publisher);
    }

    @Override
    public <T> Flux<T> requireConversation(final Flux<T> publisher) {
        return useConversation(createConversation(Conversation.Level.REQUIRED), publisher);
    }

    @Override
    public <T> Mono<T> preferConversation(final Mono<T> publisher) {
        return useConversation(createConversation(Conversation.Level.PREFERRED), publisher);
    }

    @Override
    public <T> Flux<T> preferConversation(final Flux<T> publisher) {
        return useConversation(createConversation(Conversation.Level.PREFERRED), publisher);
    }

    @Override
    public <T> Mono<T> useConversation(final Conversation conversation, final Mono<T> publisher) {
        return publisher.contextWrite(sCtx -> {
            if (sCtx.hasKey(ArangoCommunication.CONVERSATION_CTX)) {
                throw new IllegalStateException("Already existing conversation: " + sCtx.get(ArangoCommunication.CONVERSATION_CTX));
            }
            return sCtx.put(ArangoCommunication.CONVERSATION_CTX, conversation);
        });
    }

    @Override
    public <T> Flux<T> useConversation(final Conversation conversation, final Flux<T> publisher) {
        return publisher.contextWrite(sCtx -> {
            if (sCtx.hasKey(ArangoCommunication.CONVERSATION_CTX)) {
                throw new IllegalStateException("Already existing conversation: " + sCtx.get(ArangoCommunication.CONVERSATION_CTX));
            }
            return sCtx.put(ArangoCommunication.CONVERSATION_CTX, conversation);
        });
    }

}
