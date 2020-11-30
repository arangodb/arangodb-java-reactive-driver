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

package com.arangodb.reactive.api.collection.impl;


import com.arangodb.reactive.api.collection.CollectionApi;
import com.arangodb.reactive.api.collection.entity.CollectionChecksumEntity;
import com.arangodb.reactive.api.collection.entity.DetailedCollectionEntity;
import com.arangodb.reactive.api.collection.entity.SimpleCollectionEntity;
import com.arangodb.reactive.api.collection.options.CollectionChangePropertiesOptions;
import com.arangodb.reactive.api.collection.options.CollectionChecksumParams;
import com.arangodb.reactive.api.collection.options.CollectionDropParams;
import com.arangodb.reactive.api.collection.options.CollectionRenameOptions;
import com.arangodb.reactive.api.database.DatabaseApi;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.api.util.ApiPath;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.arangodb.reactive.entity.serde.SerdeTypes.STRING_LIST;
import static com.arangodb.reactive.entity.serde.SerdeTypes.STRING_OBJECT_MAP;

/**
 * @author Michele Rastelli
 */
public final class CollectionApiImpl extends ArangoClientImpl implements CollectionApi {

    private final String dbName;
    private final String colName;

    public CollectionApiImpl(final DatabaseApi arangoDatabase, final String collectionName) {
        super((ArangoClientImpl) arangoDatabase);
        dbName = arangoDatabase.getName();
        colName = collectionName;
    }

    @Override
    public String getName() {
        return colName;
    }

    @Override
    public Mono<Void> drop() {
        return drop(CollectionDropParams.builder().build());
    }

    @Override
    public Mono<Void> drop(final CollectionDropParams params) {
        return getCommunication()
                .execute(
                        ArangoRequest.builder()
                                .database(dbName)
                                .requestType(ArangoRequest.RequestType.DELETE)
                                .path(ApiPath.COLLECTION + "/" + colName)
                                .putQueryParams(
                                        CollectionDropParams.IS_SYSTEM_PARAM,
                                        params.isSystem().map(String::valueOf)
                                )
                                .build()
                )
                .then();
    }

    @Override
    public Mono<SimpleCollectionEntity> info() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName)
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserialize(bytes, SimpleCollectionEntity.class));
    }

    @Override
    public Mono<DetailedCollectionEntity> properties() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName + "/properties")
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserialize(bytes, DetailedCollectionEntity.class));
    }

    @Override
    public Mono<DetailedCollectionEntity> changeProperties(final CollectionChangePropertiesOptions options) {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/properties")
                        .body(getSerde().serialize(options))
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserialize(bytes, DetailedCollectionEntity.class));
    }

    @Override
    public Mono<SimpleCollectionEntity> rename(final CollectionRenameOptions options) {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/rename")
                        .body(getSerde().serialize(options))
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserialize(bytes, SimpleCollectionEntity.class));
    }


    @Override
    public Mono<Long> count() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName + "/count")
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeAtJsonPointer("/count", bytes, Long.class));
    }

    @Override
    public Mono<CollectionChecksumEntity> checksum() {
        return checksum(CollectionChecksumParams.builder().build());
    }

    @Override
    public Mono<CollectionChecksumEntity> checksum(final CollectionChecksumParams params) {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName + "/checksum")
                        .putQueryParams(
                                CollectionChecksumParams.WITH_REVISIONS,
                                params.getWithRevisions().map(String::valueOf)
                        )
                        .putQueryParams(
                                CollectionChecksumParams.WITH_DATA,
                                params.getWithData().map(String::valueOf)
                        )
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserialize(bytes, CollectionChecksumEntity.class));
    }

    @Override
    public Mono<Map<String, Object>> statistics() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName + "/figures")
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeAtJsonPointer("/figures", bytes, STRING_OBJECT_MAP));
    }

    @Override
    public Mono<Void> load() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/load")
                        .build())
                .then();
    }

    @Override
    public Mono<Void> loadIndexes() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/loadIndexesIntoMemory")
                        .build())
                .then();
    }

    @Override
    public Mono<Void> recalculateCount() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/recalculateCount")
                        .build())
                .then();
    }

    @Override
    public Mono<Void> truncate() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/truncate")
                        .build())
                .then();
    }

    @Override
    public Mono<String> responsibleShard(final Object document) {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/responsibleShard")
                        .body(getSerde().serialize(document))
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeAtJsonPointer("/shardId", bytes, String.class));
    }

    @Override
    public Mono<String> revision() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName + "/revision")
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().deserializeAtJsonPointer("/revision", bytes, String.class));
    }

    @Override
    public Flux<String> shards() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.GET)
                        .path(ApiPath.COLLECTION + "/" + colName + "/shards")
                        .build())
                .map(ArangoResponse::getBody)
                .map(bytes -> getSerde().<List<String>>deserializeAtJsonPointer("/shards", bytes, STRING_LIST))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Void> unload() {
        return getCommunication()
                .execute(ArangoRequest.builder()
                        .database(dbName)
                        .requestType(ArangoRequest.RequestType.PUT)
                        .path(ApiPath.COLLECTION + "/" + colName + "/unload")
                        .build())
                .then();
    }

}
