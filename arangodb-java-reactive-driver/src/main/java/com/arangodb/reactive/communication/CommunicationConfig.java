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

package com.arangodb.reactive.communication;


import com.arangodb.jackson.dataformat.velocypack.VPackMapper;
import com.arangodb.reactive.connection.ArangoProtocol;
import com.arangodb.reactive.connection.AuthenticationMethod;
import com.arangodb.reactive.connection.ConnectionConfig;
import com.arangodb.reactive.connection.ContentType;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.reactive.entity.GenerateBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Set;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
@SuppressWarnings("SameReturnValue")
public interface CommunicationConfig {
    static CommunicationConfigBuilder builder() {
        return new CommunicationConfigBuilder();
    }

    /**
     * @return ArangoDB host
     */
    Set<HostDescription> getHosts();

    /**
     * @return database deployment topology
     * @see <a href=https://www.arangodb.com/docs/stable/architecture-deployment-modes.html>Deployment Modes</a>
     */
    @Value.Default
    default ArangoTopology getTopology() {
        return ArangoTopology.SINGLE_SERVER;
    }

    /**
     * @return whether to allow dirty reads (only considered for topology {@link ArangoTopology#ACTIVE_FAILOVER})
     */
    @Value.Default
    default boolean getDirtyReads() {
        return false;
    }

    /**
     * @return connection configuration
     */
    @Value.Default
    default ConnectionConfig getConnectionConfig() {
        return ConnectionConfig.builder().build();
    }

    // TODO: mv to ArangoConfiguration
    @Value.Default
    default ContentType getContentType() {
        return ContentType.VPACK;
    }

    /**
     * @return a custom mapper to use for user data serialization and deserialization
     */
    @Value.Default
    default ObjectMapper getMapper() {
        switch (getContentType()) {
            case VPACK:
                return new VPackMapper();
            case JSON:
                return new JsonMapper();
            default:
                throw new IllegalArgumentException(String.valueOf(getContentType()));
        }
    }

    /**
     * @return network protocol
     */
    @Value.Default
    default ArangoProtocol getProtocol() {
        return ArangoProtocol.VST;
    }

    /**
     * @return whether to fetch the host list
     */
    @Value.Default
    default boolean getAcquireHostList() {
        return false;
    }

    /**
     * @return interval at which the host list will be fetched
     */
    @Value.Default
    default Duration getAcquireHostListInterval() {
        return Duration.ofMinutes(1);
    }

    /**
     * @return number of retries for every management operation, like connections creations, acquireHostList, ...
     */
    @Value.Default
    default int getRetries() {
        return 10;
    }

    /**
     * @return timeout for every management operation, like connections creations, acquireHostList, ...
     */
    @Value.Default
    default Duration getTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * @return amount of connections that will be created for every host
     */
    @Value.Default
    default int getConnectionsPerHost() {
        return 1;
    }

    /**
     * @return max number of vst threads, used by VstConnection only
     */
    @Value.Default
    default int getMaxThreads() {
        return 4;
    }

    /**
     * @return the authenticationMethod to use
     */
    // TODO: refactor to Optional<AuthenticationMethod>
    @Nullable
    AuthenticationMethod getAuthenticationMethod();

    /**
     * @return whether to negotiate the authentication (SPNEGO / Kerberos)
     */
    @Value.Default
    default boolean getNegotiateAuthentication() {
        return false;
    }

    @Value.Check
    default void checkValid() {
        if (getTimeout().compareTo(getAcquireHostListInterval()) >= 0) {
            throw new IllegalStateException("timeout must be less than acquireHostListInterval!");
        }

        if ((ContentType.VPACK.equals(getContentType()) && !getMapper().getFactory().canHandleBinaryNatively()) ||
                (ContentType.JSON.equals(getContentType()) && getMapper().getFactory().canHandleBinaryNatively())) {
            throw new IllegalStateException("Invalid mapper for the specified content type!");
        }

    }

}
