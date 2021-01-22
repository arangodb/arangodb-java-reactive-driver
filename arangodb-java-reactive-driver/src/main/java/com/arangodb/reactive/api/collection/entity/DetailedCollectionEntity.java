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

package com.arangodb.reactive.api.collection.entity;

import com.arangodb.reactive.api.collection.options.KeyOptions;
import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Michele Rastelli
 * @see <a href="https://www.arangodb.com/docs/stable/http/collection-creating.html">API Documentation</a>
 */
@GeneratePackagePrivateBuilder
@JsonDeserialize(builder = DetailedCollectionEntityBuilder.class)
@JsonIgnoreProperties({"code", "error", "id", "status", "statusString", "writeConcern"})
public interface DetailedCollectionEntity extends CollectionEntity {

    /**
     * @return whether the collection is used in a SmartGraph
     * @note Enterprise Edition only
     * TODO: test
     */
    @Nullable
    @JsonProperty("isSmart")
    Boolean isSmart();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getSmartJoinAttribute()
     */
    @Nullable
    String getSmartJoinAttribute();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getNumberOfShards()
     */
    @Nullable
    Integer getNumberOfShards();

    /**
     * @return attribute that is used in SmartGraphs
     * @note Enterprise Edition cluster only
     * TODO: test
     */
    @Nullable
    String getSmartGraphAttribute();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getReplicationFactor()
     */
    @Nullable
    ReplicationFactor getReplicationFactor();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getKeyOptions()
     */
    KeyOptions getKeyOptions();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getSchema()
     */
    @Nullable
    CollectionSchema getSchema();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getWaitForSync()
     */
    Boolean getWaitForSync();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getShardingStrategy()
     */
    @Nullable
    ShardingStrategy getShardingStrategy();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getWriteConcern()
     */
    @Nullable
    @JsonProperty("minReplicationFactor")
    Integer getWriteConcern();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getShardKeys()
     */
    @Nullable
    List<String> getShardKeys();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getDistributeShardsLike()
     */
    @Nullable
    String getDistributeShardsLike();

    /**
     * @see com.arangodb.reactive.api.collection.options.CollectionCreateOptions#getCacheEnabled()
     */
    @Nullable
    Boolean getCacheEnabled();

    /**
     * @return TODO
     * TODO: test
     */
    @Nullable
    @JsonProperty("isSmartChild")
    Boolean isSmartChild();

    /**
     * @return TODO
     * TODO: test
     */
    @Nullable
    @JsonProperty("isDisjoint")
    Boolean isDisjoint();

}
