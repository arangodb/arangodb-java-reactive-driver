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
 * @author Mark Vollmary
 * @author Michele Rastelli
 */
public enum KeyType {

    /**
     * The traditional key generator generates numerical keys in ascending order.
     */
    @JsonProperty("traditional")
    TRADITIONAL,

    /**
     * The autoincrement key generator generates numerical keys in ascending order, the initial offset and the spacing
     * can be configured
     *
     * @note single-server only
     */
    @JsonProperty("autoincrement")
    AUTOINCREMENT,

    /**
     * The padded key generator generates keys of a fixed length (16 bytes) in ascending lexicographical sort order.
     * This is ideal for usage with the RocksDB engine, which will slightly benefit keys that are inserted in
     * lexicographically ascending order. The key generator can be used in a single-server or cluster.
     */
    @JsonProperty("uuid")
    UUID,

    /**
     * The uuid key generator generates universally unique 128 bit keys, which are stored in hexadecimal human-readable
     * format. This key generator can be used in a single-server or cluster to generate “seemingly random” keys. The
     * keys produced by this key generator are not lexicographically sorted.
     */
    @JsonProperty("padded")
    PADDED

}
