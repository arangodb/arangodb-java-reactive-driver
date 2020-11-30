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
import com.arangodb.reactive.api.arangodb.ArangoDB;
import com.arangodb.reactive.api.collection.CollectionApi;
import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.reactive.ArangoClient;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
@GenerateSyncApi
public interface DatabaseApi extends ArangoClient {

    /**
     * @return database name
     */
    String getName();

    /**
     * @return main entry point for the ArangoDB driver
     */
    @SyncApiDelegator
    ArangoDB arangoDB();

    /**
     * @return CollectionApi for the current database
     */
    @SyncApiDelegator
    CollectionApi collectionApi();

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
}
