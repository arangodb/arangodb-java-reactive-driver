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


package com.arangodb.reactive.api.sync.impl;


import com.arangodb.reactive.api.reactive.ConversationManager;
import com.arangodb.reactive.api.sync.ConversationManagerSync;
import com.arangodb.reactive.api.sync.ThreadConversation;
import com.arangodb.reactive.communication.Conversation;

/**
 * @author Michele Rastelli
 */
public final class ConversationManagerSyncImpl implements ConversationManagerSync {

    private final ConversationManager cm;

    public ConversationManagerSyncImpl(final ConversationManager conversationManager) {
        cm = conversationManager;
    }

    @Override
    public ThreadConversation requireConversation() {
        return ThreadConversation.create(cm.createConversation(Conversation.Level.REQUIRED));
    }

    @Override
    public ThreadConversation preferConversation() {
        return ThreadConversation.create(cm.createConversation(Conversation.Level.PREFERRED));
    }

}
