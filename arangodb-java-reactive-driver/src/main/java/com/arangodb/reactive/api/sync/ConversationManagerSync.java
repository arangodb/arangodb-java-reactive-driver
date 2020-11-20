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


/**
 * @author Michele Rastelli
 */
public interface ConversationManagerSync {

    /**
     * Creates a new conversation and binds a ThreadLocal context to it. All the requests performed by the thread before
     * closing the conversation will be executed against the same coordinator. In case this is not possible it will
     * behave according to the specified conversation level {@link com.arangodb.reactive.communication.Conversation.Level}. Eg.:
     *
     * <pre>
     * {@code
     *         try (ThreadConversation tc = arangoDB.getConversationManager().requireConversation()) {
     *             arangoDB.createDatabase(name);
     *             DatabaseEntity db = arangoDB.getDatabase(name);
     *         }
     * }
     * </pre>
     *
     * @return an {@link AutoCloseable} {@link ThreadConversation}
     */
    ThreadConversation requireConversation();

    ThreadConversation preferConversation();

}
