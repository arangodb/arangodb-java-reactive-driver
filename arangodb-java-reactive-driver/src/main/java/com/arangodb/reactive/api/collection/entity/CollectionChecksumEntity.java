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


import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Michele Rastelli
 */
@GeneratePackagePrivateBuilder
@JsonDeserialize(builder = CollectionChecksumEntityBuilder.class)
@JsonIgnoreProperties({
        "error",
        "code",
        "isSystem",
        "type",
        "globallyUniqueId",
        "id",
        "name",
        "status"
})
public interface CollectionChecksumEntity {

    /**
     * @return The calculated checksum as a number.
     */
    String getChecksum();

    /**
     * @return The collection revision id as a string.
     */
    String getRevision();

}
