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

import com.arangodb.reactive.connection.ContentType;
import com.arangodb.reactive.exceptions.SerdeException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Callable;

/**
 * @author Michele Rastelli
 */
public abstract class ArangoSerde {

    private final ObjectMapper mapper;

    protected ArangoSerde(final ObjectMapper objectMapper) {
        this.mapper = objectMapper;
        // TODO: allow providing custom mapper (eg. configured with custom serde features)
        // TODO: set (DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true) in tests
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(ArangoDriverModule.INSTANCE.get());
    }

    public static ArangoSerde of(final ContentType contentType) {
        switch (contentType) {
            case VPACK:
                return new VPackSerde();
            case JSON:
                return new JsonSerde();
            default:
                throw new IllegalArgumentException(String.valueOf(contentType));
        }
    }

    public abstract String toJsonString(byte[] buffer);

    public final byte[] serialize(final Object value) {
        return wrapSerdeException(() ->
                mapper.writeValueAsBytes(value)
        );
    }

    public final <T> T deserialize(final byte[] buffer, final Class<T> clazz) {
        return deserialize(buffer, mapper.constructType(clazz));
    }

    public final <T> T deserialize(final byte[] buffer, final JavaType clazz) {
        return wrapSerdeException(() ->
                mapper.readerFor(clazz).readValue(buffer)
        );
    }

    public final <T> T deserializeAtJsonPointer(final String jsonPointer, final byte[] buffer, final Class<T> clazz) {
        return deserializeAtJsonPointer(jsonPointer, buffer, mapper.constructType(clazz));
    }

    public final <T> T deserializeAtJsonPointer(final String jsonPointer, final byte[] buffer, final JavaType clazz) {
        return wrapSerdeException(() ->
                mapper.readerFor(clazz).at(jsonPointer).readValue(buffer)
        );
    }

    protected final <V> V wrapSerdeException(final Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw SerdeException.builder().cause(e).build();
        }
    }

}
