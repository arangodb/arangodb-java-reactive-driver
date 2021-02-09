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


import com.arangodb.reactive.ArangoConfig;
import com.arangodb.reactive.communication.ArangoTopology;
import com.arangodb.reactive.communication.CommunicationConfig;
import com.arangodb.reactive.connection.ArangoProtocol;
import com.arangodb.reactive.connection.AuthenticationMethod;
import com.arangodb.reactive.connection.ConnectionConfig;
import com.arangodb.reactive.connection.ContentType;
import deployments.ContainerDeployment;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.extension.ExtensionContext;
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

    public final static String USER_DB = "userDb";
    public final static String USER_NAME = "user";
    public final static String USER_PASSWD = "test";

    private final ContainerDeployment deployment;
    private final ArangoConfig config;

    public static String getTestDbName(ExtensionContext extensionContext) {
        return extensionContext.getRequiredTestClass().getSimpleName();
    }

    public static String getTestCollectionName() {
        return "testCollection";
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
                .flatMap(it -> {
                    ArangoConfig rootConfig = ArangoConfig.builder()
                            .communicationConfig(CommunicationConfig.builder()
                                    .protocol(it.getKey())
                                    .contentType(it.getValue())
                                    .addAllHosts(deployment.getHosts())
                                    .authenticationMethod(deployment.getAuthentication())
                                    .topology(deployment.getTopology())
                                    .connectionConfig(ConnectionConfig
                                            .builder()
                                            .timeout(Duration.ofMillis(TestUtils.INSTANCE.getRequestTimeout()))
                                            .build())
                                    .build())
                            .build();

                    ArangoConfig userConfig = ArangoConfig.builder()
                            .adminDB(USER_DB)
                            .communicationConfig(CommunicationConfig.builder().from(rootConfig.getCommunicationConfig())
                                    .authenticationMethod(AuthenticationMethod.ofBasic(USER_NAME, USER_PASSWD))
                                    .build())
                            .build();

                            return Stream.of(rootConfig, userConfig);
                        }
                )
                .map(config -> new TestContext(deployment, config));
    }

    private TestContext(final ContainerDeployment deployment, final ArangoConfig config) {
        this.deployment = deployment;
        this.config = config;
    }

    public ContainerDeployment getDeployment() {
        return deployment;
    }

    public ArangoConfig getConfig() {
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
                config.getCommunicationConfig().getAuthenticationMethod().getUser() + ", " +
                config.getCommunicationConfig().getProtocol() + ", " +
                config.getCommunicationConfig().getContentType();
    }

}
