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

import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.api.entity.SatelliteReplicationFactor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;


/**
 * @author Michele Rastelli
 */
public final class VPackDeserializers {

    static final JsonDeserializer<ReplicationFactor> REPLICATION_FACTOR = new JsonDeserializer<ReplicationFactor>() {
        @Override
        public ReplicationFactor deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            if (JsonToken.VALUE_NUMBER_INT.equals(p.getCurrentToken())) {
                return ReplicationFactor.of(p.getValueAsInt());
            } else if (JsonToken.VALUE_STRING.equals(p.getCurrentToken())
                    && SatelliteReplicationFactor.VALUE.equals(p.getValueAsString())) {
                return ReplicationFactor.ofSatellite();
            } else {
                throw new IllegalArgumentException("Unknown value for replication factor!");
            }
        }
    };

    private VPackDeserializers() {
    }

    public static final class RawJsonDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return p.readValueAsTree().toString();
        }
    }
}
