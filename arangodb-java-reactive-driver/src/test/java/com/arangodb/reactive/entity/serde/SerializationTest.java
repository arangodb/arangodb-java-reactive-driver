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

package com.arangodb.reactive.entity.serde;

import com.arangodb.reactive.api.collection.entity.CollectionSchema;
import com.arangodb.reactive.api.database.entity.Sharding;
import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.connection.ContentType;
import com.arangodb.reactive.entity.model.Engine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
class SerializationTest {

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void collectionSchema(ContentType contentType) {
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

        testRoundTrip(
                collectionSchema,
                contentType,
                CollectionSchema.class
        );
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void satelliteReplicationFactor(ContentType contentType) {
        testRoundTrip(
                ReplicationFactor.ofSatellite(),
                contentType,
                ReplicationFactor.class
        );
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void numericReplicationFactor(ContentType contentType) {
        testRoundTrip(
                ReplicationFactor.of(3),
                contentType,
                ReplicationFactor.class
        );
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void sharding(ContentType contentType) {
        testRoundTrip(
                Sharding.of(""),
                contentType,
                Sharding.class
        );
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void storageEngineName(ContentType contentType) {
        testRoundTrip(
                Engine.StorageEngineName.ROCKSDB,
                contentType,
                Engine.StorageEngineName.class
        );
    }

    private <T> void testRoundTrip(T original, ContentType contentType, Class<? super T> clazz) {
        ArangoSerde serde = ArangoSerde.of(contentType);
        byte[] serialized = serde.serialize(original);
        System.out.println(serde.toJsonString(serialized));
        Object deserialized = serde.deserialize(serialized, clazz);
        assertThat(deserialized).isEqualTo(original);
    }

}