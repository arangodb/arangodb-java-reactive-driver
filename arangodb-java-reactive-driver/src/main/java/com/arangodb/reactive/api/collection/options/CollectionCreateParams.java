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
public interface CollectionCreateParams {

    static CollectionCreateParamsBuilder builder() {
        return new CollectionCreateParamsBuilder();
    }

    /**
     * @return if <code>true</code> the server will only report success back to the client if all replicas have created
     * the collection. Set to <code>false</code> if you want faster server responses and don't care about full replication.
     * @defaultValue <code>true</code>
     */
    Optional<Boolean> getWaitForSyncReplication();

    /**
     * @return if <code>true</code> the server will check if there are enough replicas available at creation time and
     * bail out otherwise. Set to <code>false</code> to disable this extra check.
     * @defaultValue <code>true</code>
     */
    Optional<Boolean> getEnforceReplicationFactor();

}
