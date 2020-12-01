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

package com.arangodb.reactive.api.arangodb;

import com.arangodb.codegen.GenerateSyncApi;
import com.arangodb.codegen.SyncApiDelegator;
import com.arangodb.codegen.SyncApiIgnore;
import com.arangodb.reactive.api.arangodb.impl.ArangoDBImpl;
import com.arangodb.reactive.api.database.ArangoDatabase;
import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import com.arangodb.reactive.api.reactive.ArangoClient;
import com.arangodb.reactive.communication.CommunicationConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 * @author Mark Vollmary
 */
@GenerateSyncApi
public interface ArangoDB extends ArangoClient {

    static ArangoDB create(final CommunicationConfig config) {
        return new ArangoDBImpl(config);
    }

    /**
     * @return the name of the database used to perform administration requests
     */
    String getAdminDB();

    /**
     * @return the synchronous blocking version of this object
     */
    @SyncApiIgnore
    ArangoDBSync sync();

    /**
     * @return {@link ArangoDatabase} instance for the administration database
     */
    @SyncApiDelegator
    ArangoDatabase db();

    /**
     * @param name Name of the database
     * @return {@link ArangoDatabase} instance for the given database name
     */
    @SyncApiDelegator
    ArangoDatabase db(String name);

    /**
     * Retrieves a list of all existing databases
     *
     * @return all existing databases
     * @note You should use the [GET user API] (FIXME: add javadoc link) to fetch the list of the available databases now.
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#list-of-databases">API
     * Documentation</a>
     */
    Flux<String> getDatabases();

    /**
     * Retrieves a list of all databases the current user can access
     *
     * @return all databases the current user can access
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/database-database-management.html#list-of-accessible-databases">API
     * Documentation</a>
     */
    Flux<String> getAccessibleDatabases();

    /**
     * Creates a new database with the given name.
     *
     * @param name Name of the database to create
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#create-database">API
     * Documentation</a>
     */
    @SyncApiDelegator
    Mono<ArangoDatabase> createDatabase(String name);

    /**
     * Creates a new database with the given name.
     *
     * @param options Creation options
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#create-database">API
     * Documentation</a>
     * @since ArangoDB 3.6
     */
    @SyncApiDelegator
    Mono<ArangoDatabase> createDatabase(DatabaseCreateOptions options);

    /**
     * Closes all connections and releases all the related resources.
     *
     * @return a {@link Mono} completing when done
     */
    Mono<Void> shutdown();

}
