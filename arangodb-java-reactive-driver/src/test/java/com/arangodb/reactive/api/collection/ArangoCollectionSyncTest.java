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

import com.arangodb.reactive.api.collection.entity.CollectionChecksumEntity;
import com.arangodb.reactive.api.collection.entity.CollectionSchema;
import com.arangodb.reactive.api.collection.entity.CollectionType;
import com.arangodb.reactive.api.collection.entity.DetailedCollectionEntity;
import com.arangodb.reactive.api.collection.entity.KeyType;
import com.arangodb.reactive.api.collection.entity.ShardingStrategy;
import com.arangodb.reactive.api.collection.entity.SimpleCollectionEntity;
import com.arangodb.reactive.api.collection.options.CollectionChangePropertiesOptions;
import com.arangodb.reactive.api.collection.options.CollectionCreateOptions;
import com.arangodb.reactive.api.collection.options.CollectionCreateParams;
import com.arangodb.reactive.api.collection.options.CollectionDropParams;
import com.arangodb.reactive.api.collection.options.CollectionRenameOptions;
import com.arangodb.reactive.api.collection.options.CollectionsReadParams;
import com.arangodb.reactive.api.collection.options.KeyOptions;
import com.arangodb.reactive.api.database.ArangoDatabaseSync;
import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.api.sync.ThreadConversation;
import com.arangodb.reactive.api.utils.ArangoApiTest;
import com.arangodb.reactive.api.utils.ArangoApiTestClass;
import com.arangodb.reactive.api.utils.TestContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Michele Rastelli
 */
@ArangoApiTestClass
class ArangoCollectionSyncTest {

    @ArangoApiTest
    void getCollectionsAndGetCollectionInfo(ArangoDatabaseSync database) {
        Optional<SimpleCollectionEntity> graphsOpt = database
                .collections(CollectionsReadParams.builder().excludeSystem(false).build())
                .stream()
                .filter(c -> c.getName().equals("_graphs"))
                .findFirst();

        assertThat(graphsOpt).isPresent();
        SimpleCollectionEntity graphs = graphsOpt.get();
        assertThat(graphs.getName()).isNotNull();
        assertThat(graphs.isSystem()).isTrue();
        assertThat(graphs.getType()).isEqualTo(CollectionType.DOCUMENT);
        assertThat(graphs.getGloballyUniqueId()).isNotNull();

        Optional<SimpleCollectionEntity> collection = database
                .collections(CollectionsReadParams.builder().excludeSystem(true).build())
                .stream()
                .filter(c -> c.getName().equals("_graphs"))
                .findFirst();

        assertThat(collection).isNotPresent();

        SimpleCollectionEntity graphsInfo = database.collection("_graphs").info();
        assertThat(graphsInfo).isEqualTo(graphs);
    }

    @ArangoApiTest
    void createCollectionAndGetCollectionProperties(TestContext ctx, ArangoDatabaseSync database) {
        CollectionSchema collectionSchema = CollectionSchema.builder()
                .level(CollectionSchema.Level.NEW)
                .rule(("{  " +
                        "           \"properties\": {" +
                        "               \"number\": {" +
                        "                   \"type\": \"number\"" +
                        "               }" +
                        "           }" +
                        "       }")
                        .replaceAll("\\s", ""))
                .message("The document has problems!")
                .build();

        CollectionCreateOptions options = CollectionCreateOptions.builder()
                .name("myCollection-" + UUID.randomUUID().toString())
                .replicationFactor(ReplicationFactor.of(2))
                .writeConcern(1)
                .keyOptions(KeyOptions.builder()
                        .allowUserKeys(false)
                        .type(KeyType.UUID)
                        .build()
                )
                .waitForSync(true)
                .schema(collectionSchema)
                .addShardKeys("a:")
                .numberOfShards(3)
                .isSystem(false)
                .type(CollectionType.DOCUMENT)
                .shardingStrategy(ShardingStrategy.HASH)
                .smartJoinAttribute("d")
                .cacheEnabled(true)
                .build();

        DetailedCollectionEntity createdCollection = database.createCollection(
                options,
                CollectionCreateParams.builder()
                        .enforceReplicationFactor(true)
                        .waitForSyncReplication(true)
                        .build()
        );

        assertThat(createdCollection).isNotNull();
        assertThat(createdCollection.getName()).isEqualTo(options.getName());
        assertThat(createdCollection.getKeyOptions()).isEqualTo(options.getKeyOptions());
        assertThat(createdCollection.getWaitForSync()).isEqualTo(options.getWaitForSync());
        assertThat(createdCollection.isSystem()).isEqualTo(options.isSystem());
        assertThat(createdCollection.getType()).isEqualTo(options.getType());
        assertThat(createdCollection.getGloballyUniqueId()).isNotNull();
        assertThat(createdCollection.getCacheEnabled()).isEqualTo(options.getCacheEnabled());

        if (ctx.isAtLeastVersion(3, 7)) {
            assertThat(createdCollection.getSchema()).isEqualTo(options.getSchema());
        }

        if (ctx.isCluster()) {
            assertThat(createdCollection.getReplicationFactor()).isEqualTo(options.getReplicationFactor());
            assertThat(createdCollection.getWriteConcern()).isEqualTo(options.getWriteConcern());
            assertThat(createdCollection.getShardKeys()).isEqualTo(options.getShardKeys());
            assertThat(createdCollection.getNumberOfShards()).isEqualTo(options.getNumberOfShards());
            assertThat(createdCollection.getShardingStrategy()).isEqualTo(options.getShardingStrategy());

            if (ctx.isEnterprise()) {
                assertThat(createdCollection.getSmartJoinAttribute()).isNotNull();
                CollectionCreateOptions shardLikeOptions = CollectionCreateOptions.builder()
                        .name("shardLikeCollection-" + UUID.randomUUID().toString())
                        .distributeShardsLike(options.getName())
                        .shardKeys(options.getShardKeys())
                        .build();
                DetailedCollectionEntity shardLikeCollection = database.createCollection(shardLikeOptions);
                assertThat(shardLikeCollection).isNotNull();
                assertThat(shardLikeCollection.getDistributeShardsLike()).isEqualTo(createdCollection.getName());
            }
        }

        // readCollectionProperties
        ArangoCollectionSync collection = database.collection(options.getName());
        DetailedCollectionEntity readCollectionProperties = collection.properties();
        assertThat(readCollectionProperties).isEqualTo(createdCollection);

        CollectionSchema changedCollectionSchema = CollectionSchema.builder()
                .rule(collectionSchema.getRule())
                .message("Another message!")
                .level(CollectionSchema.Level.NONE)
                .build();

        // changeCollectionProperties
        DetailedCollectionEntity changedCollectionProperties = collection.changeProperties(
                CollectionChangePropertiesOptions.builder()
                        .waitForSync(!createdCollection.getWaitForSync())
                        .schema(changedCollectionSchema)
                        .build()
        );
        assertThat(changedCollectionProperties).isNotNull();
        assertThat(changedCollectionProperties.getWaitForSync()).isEqualTo(!createdCollection.getWaitForSync());
        if (ctx.isAtLeastVersion(3, 7)) {
            assertThat(changedCollectionProperties.getSchema()).isEqualTo(changedCollectionSchema);
        }
    }

    @ArangoApiTest
    void countAndDropCollection(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(
                CollectionCreateOptions.builder().name(name).build(),
                CollectionCreateParams.builder().waitForSyncReplication(true).build()
        );

        ArangoCollectionSync collection = database.collection(name);

        // FIXME:
//        assertThat(collectionApi.existsCollection(name)).isTrue();
        assertThat(collection.count()).isZero();

        try (ThreadConversation ignored = database.getConversationManager().requireConversation()) {
            collection.drop();
            // FIXME:
//            assertThat(collectionApi.existsCollection(name)).isFalse();
        }
    }

    @ArangoApiTest
    void createAndDropSystemCollection(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(
                CollectionCreateOptions.builder().name(name).isSystem(true).build(),
                CollectionCreateParams.builder().waitForSyncReplication(true).build()
        );

        ArangoCollectionSync collection = database.collection(name);

        // FIXME:
//        assertThat(collectionApi.existsCollection(name)).isTrue();

        try (ThreadConversation ignored = database.getConversationManager().requireConversation()) {
            collection.drop(CollectionDropParams.builder().isSystem(true).build());
            // FIXME:
//            assertThat(collectionApi.existsCollection(name)).isFalse();
        }
    }

    @ArangoApiTest
    void renameCollection(TestContext ctx, ArangoDatabaseSync database) {
        assumeTrue(!ctx.isCluster(), "is not cluster");

        String name = "collection-" + UUID.randomUUID().toString();

        DetailedCollectionEntity created = database.createCollection(CollectionCreateOptions.builder().name(name).build());
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo(name);

        ArangoCollectionSync collection = database.collection(name);

        String newName = "collection-" + UUID.randomUUID().toString();
        SimpleCollectionEntity renamed = collection.rename(CollectionRenameOptions.builder().name(newName).build());
        assertThat(renamed).isNotNull();
        assertThat(renamed.getName()).isEqualTo(newName);
    }

    @ArangoApiTest
    void truncateCollection(ArangoDatabaseSync database) {

        // FIXME: add some docs to the collection

        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        collection.truncate();
        Long count = collection.count();
        assertThat(count).isEqualTo(0L);
    }

    @ArangoApiTest
    void getCollectionChecksum(TestContext ctx, ArangoDatabaseSync database) {
        assumeTrue(!ctx.isCluster(), "is not cluster");

        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        CollectionChecksumEntity collectionChecksumEntity = collection.checksum();
        assertThat(collectionChecksumEntity).isNotNull();
        assertThat(collectionChecksumEntity.getChecksum()).isNotNull();
        assertThat(collectionChecksumEntity.getRevision()).isNotNull();
    }

    @ArangoApiTest
    void getCollectionStatistics(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        Map<String, Object> collectionStatistics = collection.statistics();
        assertThat(collectionStatistics).isNotNull();
    }

    @ArangoApiTest
    void loadCollection(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        collection.load();
    }

    @ArangoApiTest
    void loadCollectionIndexes(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        collection.load();
    }

    @ArangoApiTest
    void recalculateCollectionCount(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        collection.recalculateCount();
    }

    @ArangoApiTest
    void getResponsibleShard(TestContext ctx, ArangoDatabaseSync database) {
        assumeTrue(ctx.isCluster(), "is cluster");

        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        String responsibleShard = collection.responsibleShard(Collections.singletonMap("_key", "aaa"));
        assertThat(responsibleShard).isNotNull();
    }

    @ArangoApiTest
    void getCollectionRevision(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        String revision = collection.revision();
        assertThat(revision).isNotNull();
    }

    @ArangoApiTest
    void getCollectionShards(TestContext ctx, ArangoDatabaseSync database) {
        assumeTrue(ctx.isCluster(), "is cluster");

        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        List<String> shards = collection.shards();
        assertThat(shards).isNotNull();
        assertThat(shards).isNotEmpty();
    }

    @ArangoApiTest
    void unloadCollection(ArangoDatabaseSync database) {
        String name = "collection-" + UUID.randomUUID().toString();
        database.createCollection(CollectionCreateOptions.builder().name(name).build());
        ArangoCollectionSync collection = database.collection(name);
        collection.unload();
    }

}
