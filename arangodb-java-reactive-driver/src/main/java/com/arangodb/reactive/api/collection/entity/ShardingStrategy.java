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


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Michele Rastelli
 */
public enum ShardingStrategy {

    /**
     * default sharding used by ArangoDB Community Edition before version 3.4
     */
    @JsonProperty("community-compat")
    COMMUNITY_COMPAT,

    /**
     * default sharding used by ArangoDB Enterprise Edition before version 3.4
     */
    @JsonProperty("enterprise-compat")
    ENTERPRISE_COMPAT,

    /**
     * default sharding used by smart edge collections in ArangoDB Enterprise Edition before version 3.4
     */
    @JsonProperty("enterprise-smart-edge-compat")
    ENTERPRISE_SMART_EDGE_COMPAT,

    /**
     * default sharding used for new collections starting from version 3.4 (excluding smart edge collections)
     */
    @JsonProperty("hash")
    HASH,

    /**
     * default sharding used for new smart edge collections starting from version 3.4
     */
    @JsonProperty("enterprise-hash-smart-edge")
    ENTERPRISE_HASH_SMART_EDGE

}
