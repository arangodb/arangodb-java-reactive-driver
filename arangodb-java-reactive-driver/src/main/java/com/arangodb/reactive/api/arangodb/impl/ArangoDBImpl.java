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


package com.arangodb.reactive.api.arangodb.impl;

import com.arangodb.reactive.ArangoDefaults;
import com.arangodb.reactive.api.arangodb.ArangoDB;
import com.arangodb.reactive.api.arangodb.ArangoDBSync;
import com.arangodb.reactive.api.database.ArangoDatabase;
import com.arangodb.reactive.api.database.impl.ArangoDatabaseImpl;
import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.api.util.ApiPath;
import com.arangodb.reactive.communication.CommunicationConfig;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.arangodb.reactive.api.util.ArangoResponseField.RESULT_JSON_POINTER;
import static com.arangodb.reactive.entity.serde.SerdeTypes.STRING_LIST;

/**
 * @author Michele Rastelli
 */
public final class ArangoDBImpl extends ArangoClientImpl implements ArangoDB {

    private final String adminDB;

    public ArangoDBImpl(final CommunicationConfig config) {
        super(config);
        adminDB = config.getAdminDB();
    }

    @Override
    public String getAdminDB() {
        return adminDB;
    }

    @Override
    public ArangoDBSync sync() {
        return new ArangoDBSyncImpl(this);
    }

    @Override
    public ArangoDatabase db() {
        return db(adminDB);
    }

    @Override
    public ArangoDatabase db(final String name) {
        return new ArangoDatabaseImpl(this, name);
    }

    @Override
    public Flux<String> databases() {
        return getCommunication()
                .execute(
                        ArangoRequest.builder()
                                .database(ArangoDefaults.SYSTEM_DB)
                                .requestType(ArangoRequest.RequestType.GET)
                                .path(ApiPath.DATABASE)
                                .build()
                )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde()
                        .<List<String>>deserializeAtJsonPointer(RESULT_JSON_POINTER, bytes, STRING_LIST))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<String> accessibleDatabases() {
        return getCommunication().execute(
                ArangoRequest.builder()
                        .database(adminDB)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.DATABASE + "/user")
                        .build()
        )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde()
                        .<List<String>>deserializeAtJsonPointer(RESULT_JSON_POINTER, bytes, STRING_LIST))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<ArangoDatabase> createDatabase(final String name) {
        return createDatabase(DatabaseCreateOptions.builder().name(name).build());
    }

    @Override
    public Mono<ArangoDatabase> createDatabase(final DatabaseCreateOptions options) {
        return getCommunication()
                .execute(
                        ArangoRequest.builder()
                                .database(ArangoDefaults.SYSTEM_DB)
                                .requestType(ArangoRequest.RequestType.POST)
                                .path(ApiPath.DATABASE)
                                .body(getSerde().serialize(options))
                                .build()
                )
                .thenReturn(db(options.getName()));
    }

    @Override
    public Mono<Void> shutdown() {
        return getCommunication().close();
    }

}
