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


import com.arangodb.reactive.connection.*;
import com.arangodb.reactive.exceptions.HostNotAvailableException;
import com.arangodb.reactive.exceptions.NoHostsAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.arangodb.reactive.communication.CommunicationUtils.getRandomItem;

/**
 * @author Michele Rastelli
 */
class ConnectionPoolImpl implements ConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolImpl.class);

    private final Map<HostDescription, List<ArangoConnection>> connectionsByHost;
    private final CommunicationConfig config;
    private final ConnectionFactory connectionFactory;
    private final AuthenticationMethod authentication;
    private final Semaphore updatingConnectionsSemaphore;

    ConnectionPoolImpl(
            final CommunicationConfig communicationConfig,
            final AuthenticationMethod authenticationMethod,
            final ConnectionFactory connFactory
    ) {
        LOGGER.debug("ArangoCommunicationImpl({}, {}, {})", communicationConfig, authenticationMethod, connFactory);

        config = communicationConfig;
        authentication = authenticationMethod;
        connectionFactory = connFactory;
        updatingConnectionsSemaphore = new Semaphore(1);
        connectionsByHost = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<Void> close() {
        LOGGER.debug("close()");
        List<Mono<Void>> closedConnections = connectionsByHost.values().stream()
                .flatMap(Collection::stream)
                .map(ArangoConnection::close)
                .collect(Collectors.toList());
        return Flux.merge(closedConnections).doFinally(v -> connectionFactory.close()).then();
    }

    @Override
    public Mono<ArangoResponse> execute(final ArangoRequest request) {
        ArangoConnection connection;
        try {
            HostDescription host = getRandomItem(connectionsByHost.keySet());
            LOGGER.debug("execute: picked host {}", host);
            connection = getRandomItem(connectionsByHost.get(host));
        } catch (NoSuchElementException e) {
            return Mono.error(NoHostsAvailableException.create());
        }
        return connection.execute(request);
    }

    @Override
    public Mono<ArangoResponse> execute(final ArangoRequest request, final HostDescription host) {
        List<ArangoConnection> hostConnections = connectionsByHost.get(host);
        if (hostConnections == null) {
            throw HostNotAvailableException.builder().host(host).build();
        }
        LOGGER.debug("execute: executing on host {}", host);
        ArangoConnection connection;
        try {
            connection = getRandomItem(hostConnections);
        } catch (NoSuchElementException e) {
            return Mono.error(new IOException("No open connections!"));
        }
        return connection.execute(request);
    }

    @Override
    public Mono<Void> updateConnections(final Set<HostDescription> hostList) {
        LOGGER.debug("updateConnections()");

        if (!updatingConnectionsSemaphore.tryAcquire()) {
            return Mono.error(new IllegalStateException("Ongoing updateConnections!"));
        }

        Set<HostDescription> currentHosts = connectionsByHost.keySet();

        List<Mono<Void>> addedHosts = hostList.stream()
                .filter(o -> !currentHosts.contains(o))
                .map(host -> {
                            LOGGER.debug("adding host: {}", host);
                            return Flux
                                    .merge(createHostConnections(host))
                                    .collectList()
                                    .flatMap(hostConnections -> {
                                        if (hostConnections.isEmpty()) {
                                            LOGGER.warn("not able to connect to host [{}], skipped adding host!", host);
                                            return removeHost(host);
                                        } else {
                                            connectionsByHost.put(host, hostConnections);
                                            LOGGER.debug("added host: {}", host);
                                            return Mono.empty();
                                        }
                                    });
                        }
                )
                .collect(Collectors.toList());

        List<Mono<Void>> removedHosts = currentHosts.stream()
                .filter(o -> !hostList.contains(o))
                .map(this::removeHost)
                .collect(Collectors.toList());

        return Flux.merge(Flux.merge(addedHosts), Flux.merge(removedHosts))
                .then(Mono.defer(this::removeDisconnectedHosts))
                .timeout(config.getTimeout())

                // here we cannot use Flux::doFinally since the signal is propagated downstream before the callback is
                // executed and this is a problem if a chained task re-invoke this method, eg. during {@link this#initialize}
                .doOnTerminate(() -> {
                    LOGGER.debug("updateConnections complete: {}", connectionsByHost.keySet());
                    updatingConnectionsSemaphore.release();
                })
                .doOnCancel(updatingConnectionsSemaphore::release);
    }

    @Override
    public Conversation createConversation(final Conversation.Level level) {
        try {
            HostDescription host = getRandomItem(connectionsByHost.keySet());
            return Conversation.of(host, level);
        } catch (NoSuchElementException e) {
            throw NoHostsAvailableException.create();
        }
    }

    protected Map<HostDescription, List<ArangoConnection>> getConnectionsByHost() {
        return connectionsByHost;
    }

    /**
     * removes all the hosts that are disconnected
     *
     * @return a Mono completing when the hosts have been removed
     */
    private Mono<Void> removeDisconnectedHosts() {
        List<Mono<HostDescription>> hostsToRemove = connectionsByHost.entrySet().stream()
                .map(hostConnections -> checkAllDisconnected(hostConnections.getValue())
                        .flatMap(hostDisconnected -> {
                            if (Boolean.TRUE.equals(hostDisconnected)) {
                                return Mono.just(hostConnections.getKey());
                            } else {
                                return Mono.empty();
                            }
                        }))
                .collect(Collectors.toList());

        return Flux.merge(hostsToRemove)
                .flatMap(this::removeHost)
                .then();
    }

    private Mono<Void> removeHost(final HostDescription host) {
        LOGGER.debug("removing host: {}", host);
        return Optional.ofNullable(connectionsByHost.remove(host))
                .map(this::closeHostConnections)
                .orElse(Mono.empty());
    }

    private List<Mono<ArangoConnection>> createHostConnections(final HostDescription host) {
        LOGGER.debug("createHostConnections({})", host);

        return IntStream.range(0, config.getConnectionsPerHost())
                .mapToObj(i -> Mono.defer(() -> connectionFactory.create(host, authentication))
                        .retry(config.getRetries())
                        .doOnNext(it -> LOGGER.debug("created connection to host: {}", host))
                        .checkpoint("[ConnectionPoolImpl.createHostConnections()]: cannot connect to host: " + host)
                        .doOnError(e -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.error("Error creating connection:", e);
                            } else {
                                LOGGER.error("Error creating connection: {}: {}", e.getClass().getName(), e.getMessage());
                            }
                        })
                        .onErrorResume(e -> Mono.empty()) // skips the failing connections
                )
                .collect(Collectors.toList());
    }

    private Mono<Void> closeHostConnections(final List<ArangoConnection> connections) {
        LOGGER.debug("closeHostConnections({})", connections);

        return Flux.merge(
                connections.stream()
                        .map(ArangoConnection::close)
                        .collect(Collectors.toList())
        ).then();
    }

    /**
     * @param connections to check
     * @return Mono<True> if all the provided connections are disconnected
     */
    private Mono<Boolean> checkAllDisconnected(final List<ArangoConnection> connections) {
        return Flux
                .merge(
                        connections.stream()
                                .map(ArangoConnection::isConnected)
                                .collect(Collectors.toList())
                )
                .collectList()
                .map(areConnected -> areConnected.stream().noneMatch(i -> i));
    }

    protected CommunicationConfig getConfig() {
        return config;
    }

}
