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


import com.arangodb.reactive.entity.GenerateBuilder;
import com.arangodb.reactive.entity.serde.VPackDeserializers;
import com.arangodb.reactive.entity.serde.VPackSerializers;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Michele Rastelli
 * @see <a href="https://www.arangodb.com/docs/stable/data-modeling-documents-schema-validation.html">API
 * Documentation</a>
 */
@GenerateBuilder
@JsonDeserialize(builder = CollectionSchemaBuilder.class)
public interface CollectionSchema {

    static CollectionSchemaBuilder builder() {
        return new CollectionSchemaBuilder();
    }

    /**
     * @return JSON Schema description
     */
    @JsonSerialize(using = VPackSerializers.RawJsonSerializer.class)
    @JsonDeserialize(using = VPackDeserializers.RawJsonDeserializer.class)
    String getRule();

    /**
     * @return level at which the validation will be applied
     */
    Level getLevel();

    /**
     * @return the message that will be used when validation fails
     */
    String getMessage();

    enum Level {

        /**
         * The rule is inactive and validation thus turned off.
         */
        @JsonProperty("none")
        NONE,

        /**
         * Only newly inserted documents are validated.
         */
        @JsonProperty("new")
        NEW,

        /**
         * New and modified documents must pass validation, except for modified documents where the OLD value did not
         * pass validation already. This level is useful if you have documents which do not match your target structure,
         * but you want to stop the insertion of more invalid documents and prohibit that valid documents are changed to
         * invalid documents.
         */
        @JsonProperty("moderate")
        MODERATE,

        /**
         * All new and modified document must strictly pass validation. No exceptions are made (default).
         */
        @JsonProperty("strict")
        STRICT

    }

}
