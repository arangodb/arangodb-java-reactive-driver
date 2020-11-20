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

package com.arangodb.reactive.api.database;

import com.arangodb.reactive.api.database.entity.DatabaseEntity;
import com.arangodb.reactive.api.database.entity.Sharding;
import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.api.sync.ThreadConversation;
import com.arangodb.reactive.api.utils.ArangoApiTest;
import com.arangodb.reactive.api.utils.ArangoApiTestClass;
import com.arangodb.reactive.api.utils.TestContext;
import com.arangodb.reactive.exceptions.server.ArangoServerException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


/**
 * @author Michele Rastelli
 */
@ArangoApiTestClass
class DatabaseApiSyncTest {

    @ArangoApiTest
    void createDatabase(TestContext ctx, DatabaseApiSync db) {
        String name = "db-" + UUID.randomUUID().toString();
        DatabaseEntity dbEntity;
        try (ThreadConversation ignored = db.getConversationManager().requireConversation()) {
            db.createDatabase(name);
            dbEntity = db.getDatabase(name);

            assertThat(dbEntity).isNotNull();
            assertThat(dbEntity.getId()).isNotNull();
            assertThat(dbEntity.getName()).isEqualTo(name);
            assertThat(dbEntity.getPath()).isNotNull();
            assertThat(dbEntity.isSystem()).isFalse();

            if (ctx.isCluster() && ctx.isAtLeastVersion(3, 6)) {
                assertThat(dbEntity.getWriteConcern()).isEqualTo(1);
                assertThat(dbEntity.getReplicationFactor()).isEqualTo(ReplicationFactor.of(1));
                assertThat(dbEntity.getSharding()).isEqualTo(Sharding.FLEXIBLE);
            }

            // cleanup
            db.dropDatabase(name);
        }
    }


    @ArangoApiTest
    void createAndDropDatabaseWithOptions(TestContext ctx, DatabaseApiSync db) {
        String name = "db-" + UUID.randomUUID().toString();
        DatabaseEntity dbEntity;
        try (ThreadConversation ignored = db.getConversationManager().requireConversation()) {
            db.createDatabase(DatabaseCreateOptions
                    .builder()
                    .name(name)
                    .options(DatabaseCreateOptions.Options.builder()
                            .sharding(Sharding.SINGLE)
                            .writeConcern(2)
                            .replicationFactor(ReplicationFactor.of(2))
                            .build())
                    .addUsers(DatabaseCreateOptions.DatabaseUser.builder()
                            .username("testUser-" + name)
                            .passwd("passwd")
                            .isActive(true)
                            .putExtra("key", "value")
                            .build())
                    .build());
            dbEntity = db.getDatabase(name);

            assertThat(dbEntity).isNotNull();
            assertThat(dbEntity.getId()).isNotNull();
            assertThat(dbEntity.getName()).isEqualTo(name);
            assertThat(dbEntity.getPath()).isNotNull();
            assertThat(dbEntity.isSystem()).isFalse();

            if (ctx.isCluster() && ctx.isAtLeastVersion(3, 6)) {
                assertThat(dbEntity.getWriteConcern()).isEqualTo(2);
                assertThat(dbEntity.getReplicationFactor()).isEqualTo(ReplicationFactor.of(2));
                assertThat(dbEntity.getSharding()).isEqualTo(Sharding.SINGLE);
            }

            // TODO: access db with created user

            db.dropDatabase(name);

            // get database
            Throwable thrown = catchThrowable(() -> db.getDatabase(name));

            assertThat(thrown).isInstanceOf(ArangoServerException.class);
            assertThat(thrown.getMessage()).contains("database not found");
            assertThat(((ArangoServerException) thrown).getResponseCode()).isEqualTo(404);
            assertThat(((ArangoServerException) thrown).getEntity().getErrorNum()).isEqualTo(1228);
        }
    }


    @ArangoApiTest
    void getDatabase(TestContext ctx, DatabaseApiSync db) {
        DatabaseEntity dbEntity = db.getDatabase("_system");

        assertThat(dbEntity.getId()).isNotNull();
        assertThat(dbEntity.getName()).isEqualTo("_system");
        assertThat(dbEntity.getPath()).isNotNull();
        assertThat(dbEntity.isSystem()).isTrue();

        if (ctx.isCluster() && ctx.isAtLeastVersion(3, 6)) {
            assertThat(dbEntity.getWriteConcern()).isEqualTo(1);
            assertThat(dbEntity.getReplicationFactor()).isEqualTo(ReplicationFactor.of(1));
            assertThat(dbEntity.getSharding()).isEqualTo(Sharding.FLEXIBLE);
        }
    }

    @ArangoApiTest
    void getDatabases(DatabaseApiSync db) {
        List<String> databases = db.getDatabases();
        assertThat(databases).isNotNull();
        assertThat(databases).contains("_system");
    }

    @ArangoApiTest
    void getAccessibleDatabases(DatabaseApiSync db) {
        List<String> databases = db.getAccessibleDatabases();
        assertThat(databases).isNotNull();
        assertThat(databases).contains("_system");
    }

}