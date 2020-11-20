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

import com.arangodb.reactive.api.sync.ThreadConversation;
import com.arangodb.reactive.connection.*;
import com.arangodb.reactive.entity.model.ClusterEndpoints;
import com.arangodb.reactive.entity.model.ErrorEntity;
import com.arangodb.reactive.entity.serde.ArangoSerde;
import com.arangodb.reactive.exceptions.HostNotAvailableException;
import com.arangodb.reactive.exceptions.server.ArangoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static com.arangodb.reactive.connection.ConnectionUtils.ENDPOINTS_REQUEST;

/**
 * @author Michele Rastelli
 */
@SuppressWarnings("squid:S1192")    // String literals should not be duplicated
final class ArangoCommunicationImpl implements ArangoCommunication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoCommunicationImpl.class);

    private final CommunicationConfig config;
    private final ArangoSerde serde;
    private final ConnectionFactory connectionFactory;
    private final Semaphore updatingHostListSemaphore;

    // connection pool used to acquireHostList
    private volatile ConnectionPool contactConnectionPool;

    // connection pool used for all other operations
    private volatile ConnectionPool connectionPool;

    private volatile boolean initialized = false;

    @Nullable
    private volatile AuthenticationMethod authentication;

    @Nullable
    private volatile Disposable scheduledUpdateHostListSubscription;

    ArangoCommunicationImpl(final CommunicationConfig communicationConfig, final ConnectionFactory connFactory) {
        LOGGER.debug("ArangoCommunicationImpl({}, {})", communicationConfig, connFactory);

        config = communicationConfig;
        connectionFactory = connFactory;
        updatingHostListSemaphore = new Semaphore(1);
        serde = ArangoSerde.of(communicationConfig.getContentType());
    }

    @Override
    public synchronized Mono<ArangoCommunication> initialize() {
        LOGGER.debug("initialize()");

        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        initialized = true;

        return negotiateAuthentication()
                .then(Mono.defer(() -> {
                    if (config.getAcquireHostList()) {
                        contactConnectionPool = ConnectionPool.create(
                                CommunicationConfig.builder()
                                        .from(config)
                                        .topology(ArangoTopology.SINGLE_SERVER)
                                        .connectionsPerHost(1)
                                        .build(),
                                authentication,
                                connectionFactory);

                        // create empty connectionPool, hosts will be acquired later
                        connectionPool = ConnectionPool.create(
                                CommunicationConfig.builder()
                                        .from(config)
                                        .hosts(Collections.emptyList())
                                        .build(),
                                authentication,
                                connectionFactory);

                        return contactConnectionPool.updateConnections(config.getHosts());
                    } else {
                        connectionPool = ConnectionPool.create(config, authentication, connectionFactory);
                        return updateConnections(config.getHosts());
                    }
                }))
                .then(Mono.defer(this::scheduleUpdateHostList))
                .then(Mono.just(this));
    }

    @Override
    public Mono<ArangoResponse> execute(final ArangoRequest request) {
        LOGGER.atDebug()
                .addArgument(request)
                .addArgument(() -> serde.toJsonString(request.getBody()))
                .log("execute(): {}, {}");

        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> ctx
                        .<Conversation>getOrEmpty(ArangoCommunication.CONVERSATION_CTX)
                        .map(Optional::of)
                        .orElseGet(ThreadConversation::getThreadLocalConversation)
                        .map(conversation -> execute(request, conversation))
                        .orElseGet(() -> execute(request, connectionPool)))
                .doOnNext(response -> LOGGER.atDebug()
                        .addArgument(response)
                        .addArgument(() -> serde.toJsonString(response.getBody()))
                        .log("received response: {}, {}"))
                .doOnNext(this::checkError);
    }

    @Override
    public Conversation createConversation(final Conversation.Level level) {
        return connectionPool.createConversation(level);
    }

    @Override
    public Mono<Void> close() {
        LOGGER.debug("close()");
        Optional.ofNullable(scheduledUpdateHostListSubscription).ifPresent(Disposable::dispose);
        return connectionPool.close().then();
    }

    private Mono<Void> updateConnections(final Set<HostDescription> hostList) {
        return connectionPool.updateConnections(hostList)
                // check if at least 1 coordinator is connected
                .doOnSuccess(it -> connectionPool.createConversation(Conversation.Level.REQUIRED));
    }

    private ArangoServerException buildError(final ArangoResponse response) {
        return ArangoServerException.of(
                response.getResponseCode(),
                serde.deserialize(response.getBody(), ErrorEntity.class)
        );
    }

    private void checkError(final ArangoResponse response) {
        if (response.getResponseCode() < 200 || response.getResponseCode() >= 300) {
            throw buildError(response);
        }
    }

    private Mono<ArangoResponse> execute(final ArangoRequest request, final ConnectionPool cp) {
        LOGGER.debug("execute({}, {})", request, cp);
        return Mono.defer(() -> cp.execute(request))
                .checkpoint("[ArangoCommunicationImpl.execute()]")
                .timeout(config.getTimeout());
    }

    private Mono<ArangoResponse> execute(final ArangoRequest request, final Conversation conversation) {
        LOGGER.debug("execute({}, {})", request, conversation);
        return execute(request, conversation.getHost())
                .onErrorResume(HostNotAvailableException.class, e -> {
                    if (Conversation.Level.REQUIRED.equals(conversation.getLevel())) {
                        throw e;
                    } else {
                        return execute(request, connectionPool);
                    }
                });
    }

    private Mono<ArangoResponse> execute(
            final ArangoRequest request,
            final HostDescription host
    ) {
        LOGGER.debug("execute({}, {})", request, host);
        return Mono.defer(() -> connectionPool.execute(request, host))
                .checkpoint("[ArangoCommunicationImpl.execute()]")
                .timeout(config.getTimeout());
    }

    ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    /**
     * hook to perform kerberos authentication negotiation -- for future use
     *
     * <p>
     * Implementation should overwrite this::authenticationMethod with an AuthenticationMethod that can be used
     * to connect to the db
     * </p>
     *
     * @return a {@code Mono} which completes once this::authenticationMethod has been correctly set
     */
    private Mono<Void> negotiateAuthentication() {
        LOGGER.debug("negotiateAuthentication()");

        if (config.getNegotiateAuthentication()) {
            throw new UnsupportedOperationException("Authentication Negotiation is not yet supported!");
        } else {
            authentication = config.getAuthenticationMethod();
            return Mono.empty();
        }
    }

    /**
     * Fetches from the server the host set and update accordingly the connections
     *
     * @return a {@code Mono} which completes once all these conditions are met:
     * - the connectionsByHost has been updated
     * - connections related to removed hosts have been closed
     * - connections related to added hosts have been initialized
     */
    Mono<Void> updateHostList() {
        LOGGER.debug("updateHostList()");

        if (!updatingHostListSemaphore.tryAcquire()) {
            return Mono.error(new IllegalStateException("Ongoing updateHostList!"));
        }

        return execute(ENDPOINTS_REQUEST, contactConnectionPool)
                .map(this::parseAcquireHostListResponse)
                .checkpoint("[ArangoCommunicationImpl.updateHostList()]")
                .doOnError(e -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.error("Error acquiring hostList, retrying...", e);
                    } else {
                        LOGGER.error("Error acquiring hostList, retrying... {}: {}", e.getClass().getName(), e.getMessage());
                    }
                })
                .retry(config.getRetries())
                .doOnNext(acquiredHostList -> LOGGER.debug("Acquired hosts: {}", acquiredHostList))
                .doOnError(e -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.error("Error acquiring hostList:", e);
                    } else {
                        LOGGER.error("Error acquiring hostList: {}: {}", e.getClass().getName(), e.getMessage());
                    }
                })
                .flatMap(this::updateConnections)
                .timeout(config.getTimeout())
                .doFinally(s -> updatingHostListSemaphore.release());
    }

    private Set<HostDescription> parseAcquireHostListResponse(final ArangoResponse response) {
        LOGGER.debug("parseAcquireHostListResponse({})", response);
        if (response.getResponseCode() != 200) {
            throw buildError(response);
        }
        return serde.deserialize(response.getBody(), ClusterEndpoints.class)
                .getHostDescriptions();
    }

    private Mono<Void> scheduleUpdateHostList() {
        if (config.getAcquireHostList()) {
            scheduledUpdateHostListSubscription = Flux.interval(config.getAcquireHostListInterval())
                    .flatMap(it -> updateHostList())
                    .checkpoint("[ArangoCommunicationImpl.scheduleUpdateHostList()]")
                    .subscribe();
            return updateHostList();
        } else {
            return Mono.empty();
        }
    }

}
