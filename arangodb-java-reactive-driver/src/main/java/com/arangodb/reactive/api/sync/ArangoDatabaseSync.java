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


import com.arangodb.reactive.api.collection.CollectionApiSync;
import com.arangodb.reactive.api.database.DatabaseApiSync;
import com.arangodb.reactive.api.reactive.ArangoDatabase;

/**
 * @author Michele Rastelli
 */
public interface ArangoDatabaseSync extends ArangoClientSync<ArangoDatabase> {

    /**
     * @return database name
     */
    String name();

    /**
     * @return main entry point for the ArangoDB driver
     */
    ArangoDBSync arango();

    /**
     * @return DatabaseApi for the current database
     */
    DatabaseApiSync databaseApi();

    /**
     * @return CollectionApi for the current database
     */
    CollectionApiSync collectionApi();

}
