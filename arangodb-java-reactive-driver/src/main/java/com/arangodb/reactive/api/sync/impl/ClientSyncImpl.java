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


import com.arangodb.codegen.SyncClientParentImpl;
import com.arangodb.reactive.api.reactive.ArangoClient;
import com.arangodb.reactive.api.sync.ArangoClientSync;
import com.arangodb.reactive.api.sync.ConversationManagerSync;

/**
 * @author Michele Rastelli
 */
@SyncClientParentImpl
public abstract class ClientSyncImpl<T extends ArangoClient> implements ArangoClientSync<T> {
    private final T delegate;
    private final ConversationManagerSync conversationManager;

    protected ClientSyncImpl(final T reactiveDelegate) {
        delegate = reactiveDelegate;
        conversationManager = new ConversationManagerSyncImpl(reactiveDelegate.getConversationManager());
    }

    @Override
    public final T reactive() {
        return delegate;
    }

    @Override
    public final ConversationManagerSync getConversationManager() {
        return conversationManager;
    }

}
