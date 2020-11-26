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
import com.arangodb.reactive.api.database.DatabaseApi;
import com.arangodb.reactive.api.reactive.ArangoClient;
import com.arangodb.reactive.communication.CommunicationConfig;
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
     * @return the synchronous blocking version of this object
     */
    @SyncApiIgnore
    ArangoDBSync sync();

    /**
     * Returns a {@link DatabaseApi} instance for the {@code _system} database.
     *
     * @return database handler
     */
    @SyncApiDelegator
    DatabaseApi db();

    /**
     * Returns a {@link DatabaseApi} instance for the given database name.
     *
     * @param name Name of the database
     * @return database handler
     */
    @SyncApiDelegator
    DatabaseApi db(String name);

    /**
     * Closes all connections and releases all the related resources.
     *
     * @return a {@link Mono} completing when done
     */
    Mono<Void> shutdown();

}
