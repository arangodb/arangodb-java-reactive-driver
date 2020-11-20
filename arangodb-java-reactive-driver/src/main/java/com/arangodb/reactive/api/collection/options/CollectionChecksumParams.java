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


import com.arangodb.reactive.entity.GenerateBuilder;

import java.util.Optional;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
public interface CollectionChecksumParams {

    String WITH_REVISIONS = "withRevisions";
    String WITH_DATA = "withData";

    static CollectionChecksumParamsBuilder builder() {
        return new CollectionChecksumParamsBuilder();
    }

    /**
     * @return Whether or not to include document revision ids in the checksum calculation.
     */
    Optional<Boolean> getWithRevisions();

    /**
     * @return Whether or not to include document body data in the checksum calculation.
     */
    Optional<Boolean> getWithData();

}
