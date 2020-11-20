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
import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import com.arangodb.reactive.api.reactive.ArangoClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
@GenerateSyncApi
public interface DatabaseApi extends ArangoClient {

    /**
     * Creates a new database with the given name.
     *
     * @param name Name of the database to create
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#create-database">API
     * Documentation</a>
     */
    default Mono<Void> createDatabase(String name) {
        return createDatabase(DatabaseCreateOptions.builder().name(name).build());
    }

    /**
     * Creates a new database with the given name.
     *
     * @param options Creation options
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#create-database">API
     * Documentation</a>
     * @since ArangoDB 3.6
     */
    Mono<Void> createDatabase(DatabaseCreateOptions options);

    /**
     * @param name db name
     * @return information about the database having the given name
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/database-database-management.html#information-of-the-database">API
     * Documentation</a>
     */
    Mono<DatabaseEntity> getDatabase(String name);

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
     * Deletes the database from the server.
     *
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/database-database-management.html#drop-database">API
     * Documentation</a>
     */
    Mono<Void> dropDatabase(String name);
}
