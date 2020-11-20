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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.arangodb.reactive.connection.ConnectionTestUtils.DEFAULT_SCHEDULER_FACTORY;
import static com.arangodb.reactive.connection.ConnectionTestUtils.VERSION_REQUEST;

/**
 * @author Michele Rastelli
 */
@Disabled
class ConnectionPerformanceTest {

    private final HostDescription host = HostDescription.of("172.28.3.1", 8529);
    private final AuthenticationMethod authentication = AuthenticationMethod.ofBasic("root", "test");
    private final ConnectionConfig config = ConnectionConfig.builder().build();

    @Test
    void infiniteParallelLoop() {
        int requests = 400_000;
        int connections = 4;
        long start = new Date().getTime();

        IntStream.range(0, connections)
                .mapToObj(i -> requestBatch(requests / connections))
                .collect(Collectors.toList())
                .forEach(CompletableFuture::join);

        long end = new Date().getTime();
        long elapsed = end - start;
        System.out.println("rate: " + (1_000.0 * requests / elapsed));
    }

    private CompletableFuture<Void> requestBatch(int requests) {
        return new ConnectionFactoryImpl(config,
//                ArangoProtocol.VST,
//                ArangoProtocol.HTTP11,
                ArangoProtocol.HTTP2,
                DEFAULT_SCHEDULER_FACTORY).create(host, authentication)
                .flatMapMany(connection ->
                        Flux.range(0, requests)
                                .doOnNext(i -> {
                                    if (i % 10_000 == 0)
                                        System.out.println(i);
                                })
                                .flatMap(i -> connection.execute(VERSION_REQUEST), 10)
                                .doOnNext(ConnectionTestUtils::verifyGetResponseVPack)
                )
                .then().toFuture();
    }

}
