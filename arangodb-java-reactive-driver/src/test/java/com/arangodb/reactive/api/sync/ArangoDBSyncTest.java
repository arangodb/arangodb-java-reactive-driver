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

import com.arangodb.reactive.api.arangodb.ArangoDBSync;
import com.arangodb.reactive.api.utils.ArangoApiTest;
import com.arangodb.reactive.api.utils.ArangoApiTestClass;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


/**
 * @author Michele Rastelli
 */
@ArangoApiTestClass
class ArangoDBSyncTest {

    @ArangoApiTest
    void shutdown() {
    }

    @ArangoApiTest
    void alreadyExistingThreadConversation(ArangoDBSync arango) {
        try (ThreadConversation tc = arango.getConversationManager().requireConversation()) {
            Throwable thrown = catchThrowable(() -> arango.getConversationManager().requireConversation());
            assertThat(thrown).isInstanceOf(IllegalStateException.class);
        }
    }

    @ArangoApiTest
    void wrongThreadClosingThreadConversation(ArangoDBSync arango) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, (ThreadFactory) Thread::new);
        try (ThreadConversation tc = arango.getConversationManager().requireConversation()) {
            Throwable thrown = catchThrowable(() -> CompletableFuture.runAsync(tc::close, executor).join());
            assertThat(thrown).isInstanceOf(CompletionException.class);
            assertThat(thrown.getCause()).isInstanceOf(ConcurrentModificationException.class);
        }
    }

}
