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


package com.arangodb.reactive.api.sync;


import com.arangodb.reactive.communication.Conversation;

import java.util.ConcurrentModificationException;
import java.util.Optional;

/**
 * Binds the current thread to the provided conversation. All the requests sent from this thread will happen within the
 * conversation. In case this is not possible it will behave according to the specified {@link Conversation.Level}.
 *
 * @author Michele Rastelli
 */
public final class ThreadConversation implements AutoCloseable {

    private static final ThreadLocal<Conversation> THREAD_LOCAL_CONVERSATION = new ThreadLocal<>();

    private final Conversation threadConversation;
    private final long threadId;

    private ThreadConversation(final Conversation conversation) {
        THREAD_LOCAL_CONVERSATION.set(conversation);
        threadConversation = conversation;
        threadId = Thread.currentThread().getId();
    }

    public static Optional<Conversation> getThreadLocalConversation() {
        return Optional.ofNullable(THREAD_LOCAL_CONVERSATION.get());
    }

    public static ThreadConversation create(final Conversation conversation) {
        if (getThreadLocalConversation().isPresent()) {
            throw new IllegalStateException("Already existing ThreadConversation for thread " + Thread.currentThread().getName());
        }
        return new ThreadConversation(conversation);
    }

    public Conversation getThreadConversation() {
        return threadConversation;
    }

    @Override
    public void close() {
        if (threadId != Thread.currentThread().getId()) {
            throw new ConcurrentModificationException("Thread " + Thread.currentThread().getId()
                    + " cannot close thread conversation created from thread " + threadId);
        }
        THREAD_LOCAL_CONVERSATION.remove();
    }

}
