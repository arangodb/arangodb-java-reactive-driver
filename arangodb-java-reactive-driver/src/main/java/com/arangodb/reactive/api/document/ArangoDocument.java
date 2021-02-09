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

package com.arangodb.reactive.api.document;


import com.arangodb.codegen.GenerateSyncApi;
import com.arangodb.reactive.api.document.entity.DocumentCreateEntity;
import com.arangodb.reactive.api.reactive.ArangoClient;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
@GenerateSyncApi
public interface ArangoDocument extends ArangoClient {

    /**
     * Creates a new document from the given document, unless there is already a document with the _key given. If no
     * _key is given, a new unique _key is generated automatically.
     * <p>
     * // TODO: define which types of raw data we want to let pass through (eg. VPackSlice/byte[]/String/JsonNode/ObjectNode/...)
     *
     * @param value A representation of a single document (POJO, ... )
     * @return information about the document
     * @see <a href="https://www.arangodb.com/docs/stable/http/document-working-with-documents.html#create-document">API
     * Documentation</a>
     */
    <T> Mono<DocumentCreateEntity<T>> createDocument(T value);

}
