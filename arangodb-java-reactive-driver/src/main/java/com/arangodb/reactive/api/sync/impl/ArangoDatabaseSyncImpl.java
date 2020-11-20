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


import com.arangodb.reactive.api.collection.CollectionApiSync;
import com.arangodb.reactive.api.collection.impl.CollectionApiImpl;
import com.arangodb.reactive.api.collection.impl.CollectionApiSyncImpl;
import com.arangodb.reactive.api.database.DatabaseApiSync;
import com.arangodb.reactive.api.database.impl.DatabaseApiImpl;
import com.arangodb.reactive.api.database.impl.DatabaseApiSyncImpl;
import com.arangodb.reactive.api.reactive.ArangoDatabase;
import com.arangodb.reactive.api.sync.ArangoDBSync;
import com.arangodb.reactive.api.sync.ArangoDatabaseSync;

/**
 * @author Michele Rastelli
 */
public final class ArangoDatabaseSyncImpl extends ClientSyncImpl<ArangoDatabase> implements ArangoDatabaseSync {

    public ArangoDatabaseSyncImpl(final ArangoDatabase arangoDatabase) {
        super(arangoDatabase);
    }

    @Override
    public String name() {
        return reactive().name();
    }

    @Override
    public ArangoDBSync arango() {
        return new ArangoDBSyncImpl(reactive().arango());
    }

    @Override
    public DatabaseApiSync databaseApi() {
        return new DatabaseApiSyncImpl(new DatabaseApiImpl(reactive()));
    }

    @Override
    public CollectionApiSync collectionApi() {
        return new CollectionApiSyncImpl(new CollectionApiImpl(reactive()));
    }

}
