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

package com.arangodb.reactive.api.document.entity;


import com.arangodb.reactive.entity.GenerateBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nullable;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
@JsonDeserialize(builder = DocumentCreateEntityBuilder.class)
@JsonIgnoreProperties({"new", "old"})
public interface DocumentCreateEntity<T> extends DocumentEntity {

    static <T> DocumentCreateEntityBuilder<T> builder() {
        return new DocumentCreateEntityBuilder<>();
    }

    @JsonProperty("_oldRev")
    @Nullable
    String getOldRev();

    /**
     * @return If the query parameter returnNew is true, then the complete new document is returned.
     */
    // deserialize using userSerde
    @JsonIgnore
    @Nullable
    T getNew();

    /**
     * @return If the query parameter returnOld is true, then the complete previous revision of the document is
     * returned.
     */
    // deserialize using userSerde
    @JsonIgnore
    @Nullable
    T getOld();

    @JsonIgnore
    SyncState getSyncState();

}
