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

package com.arangodb.reactive.api.database.impl;


import com.arangodb.reactive.api.arangodb.ArangoDB;
import com.arangodb.reactive.api.collection.CollectionApi;
import com.arangodb.reactive.api.collection.impl.CollectionApiImpl;
import com.arangodb.reactive.api.database.DatabaseApi;
import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.api.util.ApiPath;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import reactor.core.publisher.Mono;

import static com.arangodb.reactive.api.util.ArangoRequestParam.SYSTEM;
import static com.arangodb.reactive.api.util.ArangoResponseField.RESULT_JSON_POINTER;


/**
 * @author Michele Rastelli
 */
public final class DatabaseApiImpl extends ArangoClientImpl implements DatabaseApi {

    private final ArangoDB arango;
    private final String name;

    public DatabaseApiImpl(final ArangoDB arangoDB, final String dbName) {
        super((ArangoClientImpl) arangoDB);
        arango = arangoDB;
        this.name = dbName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArangoDB arangoDB() {
        return arango;
    }

    @Override
    public CollectionApi collectionApi() {
        return new CollectionApiImpl(this);
    }

    @Override
    public Mono<DatabaseEntity> info() {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(name)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.DATABASE + "/current")
                        .build()
        )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeAtJsonPointer(RESULT_JSON_POINTER, bytes, DatabaseEntity.class));
    }

    @Override
    public Mono<Void> drop() {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(SYSTEM)
                        .requestType(ArangoRequest.RequestType.DELETE)
                        .path(ApiPath.DATABASE + "/" + name)
                        .build()
        ).then();
    }

}
