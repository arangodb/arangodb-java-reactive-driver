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

package com.arangodb.reactive;


import com.arangodb.reactive.communication.CommunicationConfig;
import com.arangodb.reactive.entity.GenerateBuilder;
import org.immutables.value.Value;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
public interface ArangoConfig {
    static ArangoConfigBuilder builder() {
        return new ArangoConfigBuilder();
    }

    /**
     * @return database to use for administration requests. All requests performed from
     * {@link com.arangodb.reactive.api.arangodb.ArangoDB} will be scoped to this database.
     */
    @Value.Default
    default String getAdminDB() {
        return ArangoDefaults.SYSTEM_DB;
    }

    /**
     * @return communication configuration
     */
    CommunicationConfig getCommunicationConfig();

}
