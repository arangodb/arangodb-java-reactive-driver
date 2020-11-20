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

package utils;

import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.concurrent.CompletableFuture;

/**
 * @author Michele Rastelli
 */
public class EchoHttpServer {

    private static NettyOutbound echo(HttpServerRequest request, HttpServerResponse response) {
        return response
                .headers(request.requestHeaders())
                .header("uri", request.uri())
                .send(request.receive().retain());
    }

    public CompletableFuture<DisposableServer> start() {
        CompletableFuture<DisposableServer> done = new CompletableFuture<>();

        new Thread(() ->
                HttpServer.create()
                        .host("0.0.0.0")
                        .port(9000)
                        .route(routes -> routes
                                .get("/**", EchoHttpServer::echo)
                                .post("/**", EchoHttpServer::echo)
                                .put("/**", EchoHttpServer::echo)
                                .delete("/**", EchoHttpServer::echo)
                                .head("/**", EchoHttpServer::echo)
                                .options("/**", EchoHttpServer::echo))
                        .doOnBound(done::complete)
                        .bindNow().onDispose().block()
        ).start();

        return done;
    }

}
