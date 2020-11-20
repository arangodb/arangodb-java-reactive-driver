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

package com.arangodb.reactive.entity.serde;

import com.arangodb.jackson.dataformat.velocypack.VPackMapper;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Michele Rastelli
 */
public final class VPackSerde extends ArangoSerde {

    public VPackSerde() {
        super(new VPackMapper());
    }

    @Override
    public String toJsonString(final byte[] buffer) {
        try {
            return new VPackSlice(buffer).toString();
        } catch (Exception e) {
            // FIXME
            return "[INVALID VPackSlice]";
        }
    }

}
