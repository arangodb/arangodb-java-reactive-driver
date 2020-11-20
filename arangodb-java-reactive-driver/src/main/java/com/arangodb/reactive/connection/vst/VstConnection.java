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

package com.arangodb.reactive.connection.vst;

import com.arangodb.reactive.connection.*;
import com.arangodb.reactive.connection.exceptions.ArangoConnectionAuthenticationException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.netty.Connection;
import reactor.netty.DisposableChannel;
import reactor.netty.channel.AbortedException;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static com.arangodb.reactive.connection.ConnectionSchedulerFactory.THREAD_PREFIX;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

/**
 * @author Mark Vollmary
 * @author Michele Rastelli
 */
public final class VstConnection extends ArangoConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(VstConnection.class);

    private static final byte[] PROTOCOL_HEADER = "VST/1.1\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private final HostDescription host;
    private final ConnectionConfig config;
    private final MessageStore messageStore;
    private final Scheduler scheduler;
    private final VstReceiver vstReceiver;
    // mono that will be resolved when the closing process is finished
    private final Sinks.Empty<Void> closed;
    private volatile boolean initialized = false;
    private volatile boolean closing = false;
    // state managed by scheduler thread arango-vst-X
    private long mId = 0L;
    private Sinks.One<Connection> session;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;

    public VstConnection(final HostDescription hostDescription,
                         @Nullable final AuthenticationMethod authenticationMethod,
                         final ConnectionConfig connectionConfig,
                         final ConnectionSchedulerFactory schedulerFactory) {
        super(authenticationMethod);
        LOGGER.debug("VstConnection({})", connectionConfig);
        host = hostDescription;
        config = connectionConfig;
        closed = Sinks.empty();
        messageStore = new MessageStore();
        scheduler = schedulerFactory.getScheduler();
        vstReceiver = new VstReceiver(messageStore::resolve);
    }

    static void assertCorrectThread() {
        assert Thread.currentThread().getName().startsWith(THREAD_PREFIX) : "Wrong thread!";
    }

    @Override
    protected synchronized Mono<ArangoConnection> initialize() {
        LOGGER.debug("initialize()");
        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        initialized = true;
        return publishOnScheduler(this::connect).timeout(config.getTimeout())
                .then(Mono.defer(this::checkAuthenticated))
                .map(it -> this);
    }

    @Override
    public Mono<ArangoResponse> execute(final ArangoRequest request) {
        LOGGER.debug("execute({})", request);
        return publishOnScheduler(this::connect)
                .flatMap(c -> {
                    final long id = increaseAndGetMessageCounter();
                    return execute(c, id, RequestConverter.encodeRequest(id, request, config.getChunkSize()));
                })
                .timeout(config.getTimeout())
                .doOnError(this::handleError);
    }

    @Override
    public Mono<Boolean> isConnected() {
        return publishOnScheduler(() -> {
            if (connectionState == ConnectionState.CONNECTED) {
                // double check if it is still connected
                return requestUser()
                        .map(it -> true)
                        .onErrorReturn(false);
            } else {
                return Mono.just(false);
            }
        });
    }

    @Override
    public synchronized Mono<Void> close() {
        if (closing) {
            return closed.asMono();
        }
        closing = true;

        return publishOnScheduler(() -> {
            assertCorrectThread();
            LOGGER.debug("close()");
            if (connectionState == ConnectionState.DISCONNECTED) {
                return publishOnScheduler(vstReceiver::shutDown);
            } else {
                return session.asMono()
                        .doOnNext(DisposableChannel::dispose)
                        .flatMap(DisposableChannel::onDispose)
                        .publishOn(scheduler)
                        .doFinally(s -> vstReceiver.shutDown())
                        .then(closed.asMono());
            }
        });
    }

    private long increaseAndGetMessageCounter() {
        assertCorrectThread();
        return ++mId;
    }

    private ConnectionProvider createConnectionProvider() {
        return ConnectionProvider.builder("tcp")
                .maxConnections(1)
                .pendingAcquireTimeout(config.getTimeout())
                .maxIdleTime(config.getTtl())
                .build();
    }

    private Mono<Void> authenticate(final Connection connection) {
        assertCorrectThread();
        LOGGER.debug("authenticate()");
        return getAuthentication()
                .map(authenticationMethod -> {
                    final long id = increaseAndGetMessageCounter();
                    final ByteBuf buffer = RequestConverter.encodeBuffer(
                            id,
                            authenticationMethod.getVstAuthenticationMessage(),
                            config.getChunkSize()
                    );
                    return execute(connection, id, buffer)
                            .doOnNext(response -> {
                                if (response.getResponseCode() != HttpResponseStatus.OK.code()) {
                                    LOGGER.warn("in authenticate(): received response {}", response);
                                    throw ArangoConnectionAuthenticationException.of(response);
                                }
                            })
                            .then();
                })
                .orElse(Mono.empty());
    }

    private Mono<ArangoResponse> execute(final Connection connection, final long id, final ByteBuf buf) {
        assertCorrectThread();
        return send(connection, buf).then(messageStore.addRequest(id));
    }

    /**
     * This check is only useful when the connection is configured without authentication. In such case the VST
     * authentication does not happen, thus we need to check if the server is also configured without authentication.
     *
     * @return a Mono resolved if the test request is successful
     */
    private Mono<ArangoResponse> checkAuthenticated() {
        return Mono.defer(() ->
                // perform a request to check if credentials are ok
                requestUser()
                        .doOnNext(response -> {
                            if (response.getResponseCode() == HttpResponseStatus.UNAUTHORIZED.code()
                                    || response.getResponseCode() == HttpResponseStatus.FORBIDDEN.code()) {
                                throw ArangoConnectionAuthenticationException.of(response);
                            }
                        })
        );
    }

    /**
     * Executes the provided task in the scheduler.
     *
     * @param task task to execute
     * @param <T>  type returned
     * @return the supplied mono
     */
    private <T> Mono<T> publishOnScheduler(final Supplier<Mono<T>> task) {
        if (scheduler.isDisposed()) {
            return Mono.error(new IllegalStateException("Scheduler has been disposed!"));
        }
        return Mono.defer(task).subscribeOn(scheduler);
    }

    private Mono<Void> publishOnScheduler(final Runnable task) {
        if (scheduler.isDisposed()) {
            return Mono.error(new IllegalStateException("Scheduler has been disposed!"));
        }
        return Mono.defer(() -> {
            assertCorrectThread();
            task.run();
            return Mono.empty();
        }).then().subscribeOn(scheduler);
    }

    @SuppressWarnings("squid:S1872")    // Classes should not be compared by name
    private Mono<Void> send(final Connection connection, final ByteBuf buf) {
        assertCorrectThread();
        return connection.outbound()
                .send(Mono.just(buf))
                .then()
                .onErrorMap(e -> e.getClass().getSimpleName().equals("InternalNettyException"), Throwable::getCause)
                .onErrorMap(AbortedException.class, e -> new IOException(e.getCause()))
                .doOnError(t -> {
                    LOGGER.atDebug().addArgument(() -> t.getClass().getSimpleName()).log("send(ByteBuf)#doOnError({})");
                    handleError(t);
                });
    }

    private TcpClient applySslContext(final TcpClient httpClient) {
        assertCorrectThread();
        return config.getSslContext()
                .filter(v -> config.getUseSsl())
                .map(sslContext -> httpClient.secure(spec -> spec.sslContext(sslContext)))
                .orElse(httpClient);
    }

    private void handleError(final Throwable t) {
        LOGGER.atDebug().addArgument(() -> t.getClass().getSimpleName()).log("handleError({})");
        publishOnScheduler(() -> {
            assertCorrectThread();
            if (connectionState == ConnectionState.DISCONNECTED) {
                return;
            }
            connectionState = ConnectionState.DISCONNECTED;
            vstReceiver.clear();
            messageStore.clear(t);
            mId = 0L;
            if (session != null) {
                session.tryEmitError(t);
                session = null;
            }

            // completes the closing process
            if (closing) {
                closed.tryEmitEmpty();
            }

        }).subscribe();
    }

    /**
     * @return a Mono that will be resolved with a ready to use connection (connected, initialized and authenticated)
     */
    private Mono<? extends Connection> connect() {
        assertCorrectThread();
        LOGGER.debug("connect()");

        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            return session.asMono();
        } else if (connectionState == ConnectionState.DISCONNECTED) {
            // crate a pending session
            session = Sinks.one();
            connectionState = ConnectionState.CONNECTING;
            return createTcpClient()
                    .connect()
                    .publishOn(scheduler)
                    .flatMap(c -> send(c, wrappedBuffer(PROTOCOL_HEADER)).then(authenticate(c)).thenReturn(c))
                    .publishOn(scheduler)
                    .doOnNext(this::setSession);
        } else {
            throw new IllegalStateException("connectionState: " + connectionState);
        }
    }

    private TcpClient createTcpClient() {
        assertCorrectThread();
        return applySslContext(TcpClient.create(createConnectionProvider()))
                .option(CONNECT_TIMEOUT_MILLIS, Math.toIntExact(config.getTimeout().toMillis()))
                .host(host.getHost())
                .port(host.getPort())
                .doOnDisconnected(c -> handleError(new IOException("Connection closed!")))
                .handle((inbound, outbound) -> inbound
                        .receive()
                        // creates a defensive copy of the buffer to be propagate to the scheduler thread
                        .map(IOUtils::copyOf)
                        .publishOn(scheduler)
                        .doOnNext(vstReceiver::handleByteBuf)
                        .then()
                );
    }

    private void setSession(final Connection connection) {
        assertCorrectThread();
        if (session == null) {
            throw Exceptions.bubble(new IOException("Connection closed!"));
        }
        connectionState = ConnectionState.CONNECTED;
        session.tryEmitValue(connection);
    }

    private enum ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

}
