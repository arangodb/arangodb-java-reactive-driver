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


import com.arangodb.reactive.connection.http.Http11Connection;
import com.arangodb.reactive.connection.http.Http2Connection;
import com.arangodb.reactive.connection.vst.VstConnection;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
public final class ConnectionFactoryImpl implements ConnectionFactory {

    private final ConnectionConfig config;
    private final ArangoProtocol protocol;
    private final ConnectionSchedulerFactory schedulerFactory;

    /**
     * @param arangoProtocol             communication protocol
     * @param connectionConfig           connection config
     * @param connectionSchedulerFactory scheduler factory to use for VST connections
     */
    public ConnectionFactoryImpl(final ConnectionConfig connectionConfig,
                                 final ArangoProtocol arangoProtocol,
                                 final ConnectionSchedulerFactory connectionSchedulerFactory) {
        config = connectionConfig;
        protocol = arangoProtocol;
        schedulerFactory = connectionSchedulerFactory;
    }

    /**
     * @param host host
     * @return a Mono which will produce a new connection already initialized
     */
    @Override
    public Mono<ArangoConnection> create(final HostDescription host, final AuthenticationMethod authentication) {

        ArangoConnection connection;
        switch (protocol) {
            case VST:
                connection = new VstConnection(host, authentication, config, schedulerFactory);
                break;
            case HTTP11:
                connection = new Http11Connection(host, authentication, config);
                break;
            case HTTP2:
                connection = new Http2Connection(host, authentication, config);
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(protocol));
        }

        return connection.initialize()
                .doOnError(e -> connection.close().subscribe());
    }

    @Override
    public void close() {
        schedulerFactory.close();
    }

}
