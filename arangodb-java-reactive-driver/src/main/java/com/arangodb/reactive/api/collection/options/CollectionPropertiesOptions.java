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

package com.arangodb.reactive.api.collection.options;


import com.arangodb.reactive.api.collection.entity.CollectionSchema;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.annotation.Nullable;

/**
 * @author Michele Rastelli
 */
public interface CollectionPropertiesOptions {

    /**
     * @return whether the data is synchronized to disk before returning from a document create, update, replace or
     * removal operation.
     * @defaultValue <code>false</code>
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean getWaitForSync();

    /**
     * @return the collection level schema for documents
     * @since ArangoDB 3.7
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    CollectionSchema getSchema();

}
