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

package com.arangodb.reactive.api.document.impl;


import com.arangodb.reactive.api.collection.ArangoCollection;
import com.arangodb.reactive.api.document.ArangoDocument;
import com.arangodb.reactive.api.document.entity.DocumentCreateEntity;
import com.arangodb.reactive.api.document.options.DocumentCreateOptions;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.api.util.ApiPath;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
public final class ArangoDocumentImpl extends ArangoClientImpl implements ArangoDocument {

    private static final String RETURN_NEW = "returnNew";
    private static final String RETURN_OLD = "returnOld";

    private final ArangoCollection collection;

    public ArangoDocumentImpl(final ArangoCollection arangoCollection) {
        super((ArangoClientImpl) arangoCollection);
        collection = arangoCollection;
    }

    @Override
    public ArangoCollection collection() {
        return collection;
    }

    @Override
    public <T> Mono<DocumentCreateEntity<T>> createDocument(final T value, final DocumentCreateOptions options) {
        return getCommunication()
                .execute(
                        ArangoRequest.builder()
                                .database(collection.database().getName())
                                .requestType(ArangoRequest.RequestType.POST)
                                .path(ApiPath.DOCUMENT + "/" + collection.getName())
                                .putQueryParams(RETURN_NEW, options.getReturnNew().map(Object::toString))
                                .putQueryParams(RETURN_OLD, options.getReturnOld().map(Object::toString))
                                .body(getUserSerde().serialize(value))
                                .build()
                )
                .map(ArangoResponse::getBody)
                .map(bytes -> {
                    @SuppressWarnings("unchecked")
                    Class<T> type = (Class<T>) value.getClass();
                    T newValue = getUserSerde().deserializeAtJsonPointer("/new", bytes, type);
                    T oldValue = getUserSerde().deserializeAtJsonPointer("/old", bytes, type);
                    DocumentCreateEntity<?> dce = getSerde().deserialize(bytes, DocumentCreateEntity.class);
                    return DocumentCreateEntity.<T>builder()
                            .from(dce)
                            .getNew(newValue)
                            .old(oldValue)
                            .build();
                });
    }
}
