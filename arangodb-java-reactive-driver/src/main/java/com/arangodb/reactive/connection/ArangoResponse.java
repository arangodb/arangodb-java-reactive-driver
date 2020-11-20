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

package com.arangodb.reactive.connection;

import com.arangodb.reactive.entity.GenerateBuilder;
import org.immutables.value.Value;

import java.util.Map;

/**
 * @author Michele Rastelli
 * @see <a href="https://github.com/arangodb/velocystream#request--response">API</a>
 */
@GenerateBuilder
@SuppressWarnings("SameReturnValue")
public interface ArangoResponse {

    static ArangoResponseBuilder builder() {
        return new ArangoResponseBuilder();
    }

    @Value.Default
    default int getVersion() {
        return 1;
    }

    @Value.Default
    default int getType() {
        return 2;
    }

    int getResponseCode();

    Map<String, String> getMeta();

    @Value.Default
    @Value.Auxiliary
    default byte[] getBody() {
        return new byte[0];
    }
}
