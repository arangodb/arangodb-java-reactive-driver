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


import com.arangodb.reactive.ArangoDefaults;
import com.arangodb.reactive.entity.GenerateBuilder;
import io.netty.handler.ssl.SslContext;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
@SuppressWarnings("SameReturnValue")
public interface ConnectionConfig {

    static ConnectionConfigBuilder builder() {
        return new ConnectionConfigBuilder();
    }

    /**
     * @return max number of connections, used by HttpConnection only
     */
    @Value.Default
    default int getMaxConnections() {
        return 10;
    }

    /**
     * @return use SSL connection
     */
    @Value.Default
    default boolean getUseSsl() {
        return false;
    }

    /**
     * @return sslContext to use
     */
    Optional<SslContext> getSslContext();

    @Value.Default
    default ContentType getContentType() {
        return ContentType.VPACK;
    }

    /**
     * @return connect, request and pool acquisition timeout
     */
    @Value.Default
    default Duration getTimeout() {
        return Duration.ofMillis(ArangoDefaults.DEFAULT_TIMEOUT);
    }

    /**
     * @return the {@link Duration} after which the channel will be closed
     */
    @Value.Default
    default Duration getTtl() {
        return Duration.ofMillis(ArangoDefaults.DEFAULT_TTL);
    }

    /**
     * @return VelocyStream Chunk content-size (bytes), used by VstConnection only
     */
    @Value.Default
    default int getChunkSize() {
        return ArangoDefaults.CHUNK_DEFAULT_CONTENT_SIZE;
    }

    /**
     * @return whether the connection should resend the received cookies and honour the related maxAge, used by
     * HttpConnection only
     */
    @Value.Default
    default boolean getResendCookies() {
        return true;
    }

}
