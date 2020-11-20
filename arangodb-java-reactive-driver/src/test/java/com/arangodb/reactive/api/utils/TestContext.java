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

package com.arangodb.reactive.api.utils;


import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.communication.CommunicationConfig;
import com.arangodb.reactive.connection.ArangoProtocol;
import com.arangodb.reactive.connection.ConnectionConfig;
import com.arangodb.reactive.connection.ContentType;
import deployments.ContainerDeployment;
import org.assertj.core.data.MapEntry;
import utils.TestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Michele Rastelli
 */
public class TestContext {

    private final ContainerDeployment deployment;
    private final CommunicationConfig config;

    private TestContext(final ContainerDeployment deployment, final CommunicationConfig config) {
        this.deployment = deployment;
        this.config = config;
    }

    public static Stream<TestContext> createContexts(final ContainerDeployment deployment) {
        List<Map.Entry<ArangoProtocol, ContentType>> contexts = new ArrayList<>();
        contexts.add(MapEntry.entry(ArangoProtocol.VST, ContentType.VPACK));
        contexts.add(MapEntry.entry(ArangoProtocol.HTTP11, ContentType.VPACK));
        contexts.add(MapEntry.entry(ArangoProtocol.HTTP11, ContentType.JSON));

        if (deployment.isAtLeastVersion(3, 7)) {
            contexts.add(MapEntry.entry(ArangoProtocol.HTTP2, ContentType.VPACK));
            contexts.add(MapEntry.entry(ArangoProtocol.HTTP2, ContentType.JSON));
        }

        return contexts.stream()
                .map(it -> CommunicationConfig.builder()
                        .protocol(it.getKey())
                        .contentType(it.getValue())
                        .addAllHosts(deployment.getHosts())
                        .authenticationMethod(deployment.getAuthentication())
                        .topology(deployment.getTopology())
                        .connectionConfig(ConnectionConfig
                                .builder()
                                .timeout(Duration.ofMillis(TestUtils.INSTANCE.getRequestTimeout()))
                                .build())
                        .build()
                )
                .map(config -> new TestContext(deployment, config));
    }

    public CommunicationConfig getConfig() {
        return config;
    }

    public boolean isEnterprise() {
        return deployment.isEnterprise();
    }

    public boolean isCluster() {
        return deployment.getTopology().equals(ArangoTopology.CLUSTER);
    }

    public boolean isAtLeastVersion(int major, int minor) {
        return deployment.isAtLeastVersion(major, minor);
    }

    @Override
    public String toString() {
        return deployment.getTopology() + ", " +
                config.getProtocol() + ", " +
                config.getContentType();
    }

}
