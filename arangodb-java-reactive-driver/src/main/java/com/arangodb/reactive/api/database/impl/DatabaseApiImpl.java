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


import com.arangodb.reactive.api.database.DatabaseApi;
import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import com.arangodb.reactive.api.reactive.ArangoDatabase;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.arangodb.reactive.api.util.ArangoRequestParam.SYSTEM;
import static com.arangodb.reactive.api.util.ArangoResponseField.RESULT_JSON_POINTER;


/**
 * @author Michele Rastelli
 */
public final class DatabaseApiImpl extends ArangoClientImpl implements DatabaseApi {

    private static final String PATH_API = "/_api/database";
    private final String dbName;

    public DatabaseApiImpl(final ArangoDatabase arangoDatabase) {
        super((ArangoClientImpl) arangoDatabase);
        dbName = arangoDatabase.name();
    }

    @Override
    public Mono<Void> createDatabase(final DatabaseCreateOptions options) {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(SYSTEM)
                        .requestType(ArangoRequest.RequestType.POST)
                        .path(PATH_API)
                        .body(getSerde().serialize(options))
                        .build()
        ).then();
    }

    @Override
    public Mono<DatabaseEntity> getDatabase(final String name) {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(name)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(PATH_API + "/current")
                        .build()
        )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeAtJsonPointer(RESULT_JSON_POINTER, bytes, DatabaseEntity.class));
    }

    @Override
    public Flux<String> getDatabases() {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(SYSTEM)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(PATH_API)
                        .build()
        )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeListAtJsonPointer(RESULT_JSON_POINTER, bytes, String.class))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<String> getAccessibleDatabases() {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(PATH_API + "/user")
                        .build()
        )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeListAtJsonPointer(RESULT_JSON_POINTER, bytes, String.class))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Void> dropDatabase(final String name) {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(SYSTEM)
                        .requestType(ArangoRequest.RequestType.DELETE)
                        .path(PATH_API + "/" + name)
                        .build()
        ).then();
    }

}
