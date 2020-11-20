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

import com.arangodb.reactive.connection.ArangoProtocol;
import com.arangodb.reactive.connection.AuthenticationMethod;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.velocypack.VPackSlice;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Date;

import static com.arangodb.reactive.connection.ConnectionTestUtils.VERSION_REQUEST;

/**
 * @author Michele Rastelli
 */
@Disabled
class CommunicationPerformanceTest {

    private final HostDescription host = HostDescription.of("172.28.3.1", 8529);
    private final AuthenticationMethod authentication = AuthenticationMethod.ofBasic("root", "test");
    private final CommunicationConfig config = CommunicationConfig.builder()
            .addHosts(host)
            .authenticationMethod(authentication)
            .connectionsPerHost(4)
            .acquireHostList(false)
            .protocol(ArangoProtocol.VST)
//            .protocol(ArangoProtocol.HTTP11)
//            .protocol(ArangoProtocol.HTTP2)
            .build();

    private volatile long chunkStart;

    @Test
    @SuppressWarnings("squid:S2699")
        // Tests should include assertions
    void infiniteParallelLoop() {
        int requests = 1_000_000;
        int chunkSize = 100_000;
        chunkStart = new Date().getTime();

        long start = new Date().getTime();
        ArangoCommunication.create(config)
                .flatMapMany(communication -> Flux.range(1, requests)
                        .doOnNext(i -> {
                            if (i % chunkSize == 0) {
                                System.out.println(i);
                                long chunkRate = chunkSize * 1000 / (new Date().getTime() - chunkStart);
                                System.out.println("rate: " + chunkRate + " reqs/s");
                                chunkStart = new Date().getTime();
                            }
                        })
                        .flatMap(i -> communication.execute(VERSION_REQUEST), 20)
                        .doOnNext(v -> new VPackSlice(v.getBody()).get("server"))
                )
                .then()
                .block();

        long end = new Date().getTime();
        long elapsed = end - start;
        System.out.println("rate: " + (1_000.0 * requests / elapsed));
    }


}
