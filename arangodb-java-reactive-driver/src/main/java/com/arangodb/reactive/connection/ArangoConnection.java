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
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Optional;


/**
 * @author Michele Rastelli
 */
public abstract class ArangoConnection {

    @Nullable
    private final AuthenticationMethod authentication;
    private final ArangoRequest userRequest;

    protected ArangoConnection(@Nullable final AuthenticationMethod authenticationMethod) {
        authentication = authenticationMethod;
        userRequest = ArangoRequest.builder()
                .database(ArangoDefaults.SYSTEM_DB)
                .path("/_api/user/" + getAuthentication().map(AuthenticationMethod::getUser).orElse("root"))
                .requestType(ArangoRequest.RequestType.GET)
                .build();
    }

    protected final Optional<AuthenticationMethod> getAuthentication() {
        return Optional.ofNullable(authentication);
    }

    /**
     * Initializes the connection asynchronously, eg. establishing the tcp connection and performing the authentication
     *
     * @return the connection ready to be used
     */
    protected abstract Mono<ArangoConnection> initialize();

    /**
     * Performs a request
     *
     * @param request to send
     * @return response from the server
     */
    public abstract Mono<ArangoResponse> execute(ArangoRequest request);

    /**
     * @return whether the connection is open or closed
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * @return a mono completing once the connection is closed
     */
    public abstract Mono<Void> close();

    /**
     * Executes a request to /_api/user/{username}
     *
     * @return server response
     */
    public final Mono<ArangoResponse> requestUser() {
        return execute(userRequest);
    }

}
