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

package com.arangodb.reactive.api.database;


import com.arangodb.codegen.GenerateSyncApi;
import com.arangodb.codegen.SyncApiDelegator;
import com.arangodb.reactive.api.collection.ArangoCollection;
import com.arangodb.reactive.api.collection.entity.DetailedCollectionEntity;
import com.arangodb.reactive.api.collection.entity.SimpleCollectionEntity;
import com.arangodb.reactive.api.collection.options.CollectionCreateOptions;
import com.arangodb.reactive.api.collection.options.CollectionCreateParams;
import com.arangodb.reactive.api.collection.options.CollectionsReadParams;
import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.reactive.ArangoClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
@GenerateSyncApi
public interface ArangoDatabase extends ArangoClient {

    /**
     * @return database name
     */
    String getName();

    /**
     * @param collectionName Name of the collection
     * @return CollectionApi for the current database
     */
    @SyncApiDelegator
    ArangoCollection collection(String collectionName);

    /**
     * @return information about the database
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/database-database-management.html#information-of-the-database">API
     * Documentation</a>
     */
    Mono<DatabaseEntity> info();

    /**
     * Deletes the database from the server.
     *
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#drop-database">API
     * Documentation</a>
     */
    Mono<Void> drop();

    /**
     * @return all non-system collections description
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-getting.html#reads-all-collections">API
     * Documentation</a>
     */
    Flux<SimpleCollectionEntity> collections();

    /**
     * @param options request options
     * @return all collections description
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-getting.html#reads-all-collections">API
     * Documentation</a>
     */
    Flux<SimpleCollectionEntity> collections(CollectionsReadParams options);

    /**
     * Creates a collection for the given collection name and returns related information from the server.
     *
     * @param options request options
     * @return information about the collection
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-creating.html#create-collection">API
     * Documentation</a>
     */
    Mono<DetailedCollectionEntity> createCollection(CollectionCreateOptions options);

    /**
     * Creates a collection for the given collection name and returns related information from the server.
     *
     * @param options request options
     * @param params  request params
     * @return information about the collection
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-creating.html#create-collection">API
     * Documentation</a>
     */
    Mono<DetailedCollectionEntity> createCollection(CollectionCreateOptions options, CollectionCreateParams params);

}
