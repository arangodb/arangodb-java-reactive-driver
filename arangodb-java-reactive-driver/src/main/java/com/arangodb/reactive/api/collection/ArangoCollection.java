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

package com.arangodb.reactive.api.collection;


import com.arangodb.codegen.GenerateSyncApi;
import com.arangodb.codegen.SyncApiDelegator;
import com.arangodb.reactive.api.collection.entity.CollectionChecksumEntity;
import com.arangodb.reactive.api.collection.entity.DetailedCollectionEntity;
import com.arangodb.reactive.api.collection.entity.SimpleCollectionEntity;
import com.arangodb.reactive.api.collection.options.CollectionChangePropertiesOptions;
import com.arangodb.reactive.api.collection.options.CollectionChecksumParams;
import com.arangodb.reactive.api.collection.options.CollectionDropParams;
import com.arangodb.reactive.api.collection.options.CollectionRenameOptions;
import com.arangodb.reactive.api.database.ArangoDatabase;
import com.arangodb.reactive.api.document.ArangoDocument;
import com.arangodb.reactive.api.reactive.ArangoClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author Michele Rastelli
 * @see <a href="https://www.arangodb.com/docs/stable/http/collection.html">API Documentation</a>
 */
@GenerateSyncApi
public interface ArangoCollection extends ArangoClient {

    /**
     * @return collection name
     */
    String getName();

    /**
     * @return ArangoDatabase for the current collection
     */
    @SyncApiDelegator
    ArangoDatabase database();

    /**
     * @return DocumentApi for the current collection
     */
    @SyncApiDelegator
    ArangoDocument document();

    /**
     * Deletes the collection from the database.
     *
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-creating.html#drops-a-collection">API
     * Documentation</a>
     */
    Mono<Void> drop();

    /**
     * Deletes the collection from the database.
     *
     * @param params request params
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-creating.html#drops-a-collection">API
     * Documentation</a>
     */
    Mono<Void> drop(CollectionDropParams params);

    /**
     * @return information about the collection
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-information-about-a-collection">API
     * Documentation</a>
     */
    Mono<SimpleCollectionEntity> info();

    /**
     * @return properties of the collection
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#read-properties-of-a-collection">API
     * Documentation</a>
     */
    Mono<DetailedCollectionEntity> properties();

    /**
     * Changes the properties of the collection
     *
     * @param options request options
     * @return information about the collection
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-modifying.html#change-properties-of-a-collection">API
     * Documentation</a>
     */
    Mono<DetailedCollectionEntity> changeProperties(CollectionChangePropertiesOptions options);

    /**
     * Renames the collection
     *
     * @param options request options
     * @return information about the collection
     * @note single-server only
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-modifying.html#rename-collection">API
     * Documentation</a>
     */
    Mono<SimpleCollectionEntity> rename(CollectionRenameOptions options);

    /**
     * @return the count of documents in the collection
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-number-of-documents-in-a-collection">API
     * Documentation</a>
     */
    Mono<Long> count();

    /**
     * @return checksum for the specified collection
     * @note single-server only
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-checksum-for-the-collection">API
     * Documentation</a>
     */
    Mono<CollectionChecksumEntity> checksum();

    /**
     * @param params request params
     * @return checksum for the specified collection
     * @note single-server only
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-checksum-for-the-collection">API
     * Documentation</a>
     */
    Mono<CollectionChecksumEntity> checksum(CollectionChecksumParams params);

    /**
     * @return statistics for the specified collection
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-statistics-for-a-collection">API
     * Documentation</a>
     */
    Mono<Map<String, Object>> statistics();

    /**
     * Loads a collection indexes into memory.
     *
     * @return a Mono completing on operation completion
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-modifying.html#load-indexes-into-memory">API
     * Documentation</a>
     */
    Mono<Void> loadIndexes();

    /**
     * Recalculates the document count of a collection
     *
     * @return a Mono completing on operation completion
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-modifying.html#recalculate-count-of-a-collection">API
     * Documentation</a>
     */
    Mono<Void> recalculateCount();

    /**
     * Removes all documents from the collection, but leaves the indexes intact
     *
     * @return a Mono completing on operation completion
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-creating.html#truncate-collection">API
     * Documentation</a>
     */
    Mono<Void> truncate();

    /**
     * @param document A projection of the document containing at least the shard key (_key or a custom attribute) for
     *                 which the responsible shard should be determined
     * @return Returns the ID of the shard that is responsible for the given document (if the document exists) or that
     * would be responsible if such document existed
     * @see <a href="https://www.arangodb.com/docs/stable/http/collection-getting.html#return-responsible-shard-for-a-document">API
     * Documentation</a>
     */
    Mono<String> responsibleShard(Object document);

    /**
     * @return The revision id is a server-generated string that clients can use to check whether data in a collection
     * has changed since the last revision check.
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-collection-revision-id">API
     * Documentation</a>
     */
    Mono<String> revision();

    /**
     * @return shard IDs of the collection
     * @see <a href=
     * "https://www.arangodb.com/docs/stable/http/collection-getting.html#return-the-shard-ids-of-a-collection">API
     * Documentation</a>
     */
    Flux<String> shards();

}
