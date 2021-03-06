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

import deployments.ContainerDeployment;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import utils.ArangoTls13SupportExtension;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.arangodb.reactive.connection.ConnectionTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
@Testcontainers
@EnabledForJreRange(min = JRE.JAVA_11)
@ExtendWith(ArangoTls13SupportExtension.class)
class SslTls13ConnectionTest {

    private static final String SSL_TRUSTSTORE = "/example.truststore";
    private static final String SSL_TRUSTSTORE_PASSWORD = "12345678";
    @Container
    private static final ContainerDeployment deployment = ContainerDeployment.ofSingleServerWithSslTls13();
    private static HostDescription host;
    private final ConnectionConfig config;

    SslTls13ConnectionTest() throws Exception {
        config = ConnectionConfig.builder()
                .useSsl(true)
                .sslContext(getSslContext())
                .build();
    }

    /**
     * Provided arguments are:
     * - ArangoProtocol
     * - AuthenticationMethod
     */
    static private Stream<Arguments> argumentsProvider() throws IOException {
        List<ArangoProtocol> protocols = new ArrayList<>();
        protocols.add(ArangoProtocol.VST);
        protocols.add(ArangoProtocol.HTTP11);

        if (deployment.isAtLeastVersion(3, 7)) {
            protocols.add(ArangoProtocol.HTTP2);
        }

        AuthenticationMethod authentication = deployment.getAuthentication();
        AuthenticationMethod jwtAuthentication = deployment.getJwtAuthentication();
        return protocols.stream()
                .flatMap(p -> Stream.of(
                        Arguments.of(p, authentication),
                        Arguments.of(p, jwtAuthentication)));
    }

    @BeforeAll
    static void setup() {
        host = deployment.getHosts().get(0);
    }

    private static SslContext getSslContext() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(SslTls13ConnectionTest.class.getResourceAsStream(SSL_TRUSTSTORE), SSL_TRUSTSTORE_PASSWORD.toCharArray());

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, SSL_TRUSTSTORE_PASSWORD.toCharArray());

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        return SslContextBuilder
                .forClient()
                .protocols("TLSv1.3")
                .sslProvider(SslProvider.JDK)
                .trustManager(tmf)
                .build();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void getRequest(ArangoProtocol protocol, AuthenticationMethod authenticationMethod) {
        ArangoConnection connection = new ConnectionFactoryImpl(config, protocol, DEFAULT_SCHEDULER_FACTORY)
                .create(host, authenticationMethod).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.VERSION_REQUEST).block();
        verifyGetResponseVPack(response);
        connection.close().block();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void postRequest(ArangoProtocol protocol, AuthenticationMethod authenticationMethod) {
        ArangoConnection connection = new ConnectionFactoryImpl(config, protocol, DEFAULT_SCHEDULER_FACTORY)
                .create(host, authenticationMethod).block();
        assertThat(connection).isNotNull();
        ArangoResponse response = connection.execute(ConnectionTestUtils.postRequest()).block();
        verifyPostResponseVPack(response);
        connection.close().block();
    }

}