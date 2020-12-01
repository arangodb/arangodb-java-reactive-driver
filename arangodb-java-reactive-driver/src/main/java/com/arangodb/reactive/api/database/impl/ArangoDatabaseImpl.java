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
import com.arangodb.reactive.api.collection.ArangoCollection;
import com.arangodb.reactive.api.collection.entity.DetailedCollectionEntity;
import com.arangodb.reactive.api.collection.entity.SimpleCollectionEntity;
import com.arangodb.reactive.api.collection.impl.ArangoCollectionImpl;
import com.arangodb.reactive.api.collection.options.CollectionCreateOptions;
import com.arangodb.reactive.api.collection.options.CollectionCreateParams;
import com.arangodb.reactive.api.collection.options.CollectionsReadParams;
import com.arangodb.reactive.api.database.ArangoDatabase;
import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.api.util.ApiPath;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static com.arangodb.reactive.api.util.ArangoResponseField.RESULT_JSON_POINTER;


/**
 * @author Michele Rastelli
 */
public final class ArangoDatabaseImpl extends ArangoClientImpl implements ArangoDatabase {

    private static final JavaType SIMPLE_COLLECTION_LIST = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, SimpleCollectionEntity.class);

    private final ArangoDB arango;
    private final String name;

    public ArangoDatabaseImpl(final ArangoDB arangoDB, final String dbName) {
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
    public ArangoCollection collection(final String collectionName) {
        return new ArangoCollectionImpl(this, collectionName);
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
                        .database(arango.getAdminDB())
                        .requestType(ArangoRequest.RequestType.DELETE)
                        .path(ApiPath.DATABASE + "/" + name)
                        .build()
        ).then();
    }

    @Override
    public Flux<SimpleCollectionEntity> getCollections() {
        return getCollections(CollectionsReadParams.builder().excludeSystem(true).build());
    }

    @Override
    public Flux<SimpleCollectionEntity> getCollections(final CollectionsReadParams params) {
        return getCommunication()
                .execute(
                        ArangoRequest.builder()
                                .database(name)
                                .requestType(ArangoRequest.RequestType.GET)
                                .path(ApiPath.COLLECTION)
                                .putQueryParams(
                                        CollectionsReadParams.EXCLUDE_SYSTEM_PARAM,
                                        params.getExcludeSystem().map(String::valueOf)
                                )
                                .build()
                )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde()
                        .<List<SimpleCollectionEntity>>deserializeAtJsonPointer(RESULT_JSON_POINTER, bytes, SIMPLE_COLLECTION_LIST))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<DetailedCollectionEntity> createCollection(final CollectionCreateOptions options) {
        return createCollection(options, CollectionCreateParams.builder().build());
    }

    @Override
    public Mono<DetailedCollectionEntity> createCollection(
            final CollectionCreateOptions options,
            final CollectionCreateParams params
    ) {
        return getCommunication()
                .execute(
                        ArangoRequest.builder()
                                .database(name)
                                .requestType(ArangoRequest.RequestType.POST)
                                .body(getSerde().serialize(options))
                                .path(ApiPath.COLLECTION)
                                .putQueryParams(
                                        "enforceReplicationFactor",
                                        params.getEnforceReplicationFactor().map(it -> it ? "1" : "0")
                                )
                                .putQueryParams(
                                        "waitForSyncReplication",
                                        params.getWaitForSyncReplication().map(it -> it ? "1" : "0")
                                )
                                .build()
                )
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserialize(bytes, DetailedCollectionEntity.class));
    }

}
