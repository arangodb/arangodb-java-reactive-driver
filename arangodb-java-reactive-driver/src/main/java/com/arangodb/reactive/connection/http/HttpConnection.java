/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.reactive.connection.http;

import com.arangodb.reactive.connection.ArangoConnection;
import com.arangodb.reactive.connection.ArangoRequest;
import com.arangodb.reactive.connection.ArangoResponse;
import com.arangodb.reactive.connection.AuthenticationMethod;
import com.arangodb.reactive.connection.ConnectionConfig;
import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.reactive.connection.IOUtils;
import com.arangodb.reactive.connection.exceptions.ArangoConnectionAuthenticationException;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

import javax.annotation.Nullable;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author Mark Vollmary
 * @author Michele Rastelli
 */

abstract class HttpConnection extends ArangoConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnection.class);

    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_VPACK = "application/x-velocypack";

    private final HostDescription host;
    private final ConnectionProvider connectionProvider;
    private final HttpClient client;
    private final ConnectionConfig config;
    private final CookieStore cookieStore;
    private volatile boolean initialized = false;
    private volatile boolean connected = false;

    protected HttpConnection(final HostDescription hostDescription,
                             @Nullable final AuthenticationMethod authenticationMethod,
                             final ConnectionConfig connectionConfig) {
        super(authenticationMethod);
        LOGGER.debug("HttpConnection({})", connectionConfig);
        host = hostDescription;
        config = connectionConfig;
        connectionProvider = createConnectionProvider();
        client = getClient();
        cookieStore = new CookieStore();
    }

    private static String buildUrl(final ArangoRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append("/_db/").append(request.getDatabase());
        sb.append(request.getPath());

        final String paramString = request.getQueryParams().entrySet().stream()
                .filter(it -> it.getValue().isPresent())
                .map(it -> it.getKey() + "=" + it.getValue().get())
                .collect(Collectors.joining("&"));

        if (!paramString.isEmpty()) {
            sb.append("?");
            sb.append(paramString);
        }
        return sb.toString();
    }

    private static void addHeaders(final ArangoRequest request, final HttpHeaders headers) {
        for (final Entry<String, String> header : request.getHeaderParams().entrySet()) {
            headers.add(header.getKey(), header.getValue());
        }
    }

    protected abstract HttpProtocol getProtocol();

    protected ConnectionConfig getConfig() {
        return config;
    }

    @Override
    protected synchronized Mono<ArangoConnection> initialize() {
        LOGGER.debug("initialize()");
        if (initialized) {
            throw new IllegalStateException("Already initialized!");
        }
        initialized = true;

        // perform a request to check if credentials are ok
        return requestUser()
                .map(response -> {
                    if (response.getResponseCode() == HttpResponseStatus.UNAUTHORIZED.code()
                            || response.getResponseCode() == HttpResponseStatus.FORBIDDEN.code()) {
                        connected = false;
                        throw ArangoConnectionAuthenticationException.of(response);
                    }
                    return response;
                })
                .map(it -> this);
    }

    @Override
    public Mono<ArangoResponse> execute(final ArangoRequest request) {
        LOGGER.debug("execute({})", request);
        final String url = buildUrl(request);
        return createHttpClient(request, request.getBody().length)
                .request(requestTypeToHttpMethod(request.getRequestType())).uri(url)
                .send(Mono.just(IOUtils.createBuffer(request.getBody())))
                .responseSingle(this::buildResponse)
                .timeout(config.getTimeout())
                .doOnNext(response -> connected = true)
                .doOnError(throwable -> close().subscribe());
    }

    @Override
    public Mono<Boolean> isConnected() {
        if (connected) {
            // double check if it is still connected
            return requestUser()
                    .map(it -> true)
                    .onErrorReturn(false);
        } else {
            return Mono.just(false);
        }
    }

    @Override
    public Mono<Void> close() {
        LOGGER.debug("close()");
        return connectionProvider.disposeLater()
                .doOnTerminate(() -> {
                    connected = false;
                    cookieStore.clear();
                });
    }

    private ConnectionProvider createConnectionProvider() {
        return ConnectionProvider.builder("http")
                .maxConnections(config.getMaxConnections())
                .pendingAcquireTimeout(config.getTimeout())
                .maxIdleTime(config.getTtl())
                .build();
    }

    private HttpClient getClient() {
        return applySslContext(
                HttpClient
                        .create(connectionProvider)
                        .responseTimeout(config.getTimeout())
                        .protocol(getProtocol())
                        .keepAlive(true)
                        .baseUrl((config.getUseSsl() ? "https://" : "http://") + host.getHost() + ":" + host.getPort())
                        .headers(headers -> getAuthentication().ifPresent(
                                method -> headers.set(HttpHeaderNames.AUTHORIZATION, method.getHttpAuthorizationHeader())
                        ))
        );
    }

    private HttpClient applySslContext(final HttpClient httpClient) {
        if (config.getUseSsl() && config.getSslContext().isPresent()) {
            return httpClient.secure(spec -> spec.sslContext(config.getSslContext().get()));
        } else {
            return httpClient;
        }
    }

    private HttpMethod requestTypeToHttpMethod(final ArangoRequest.RequestType requestType) {
        switch (requestType) {
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case PATCH:
                return HttpMethod.PATCH;
            case DELETE:
                return HttpMethod.DELETE;
            case HEAD:
                return HttpMethod.HEAD;
            case GET:
                return HttpMethod.GET;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String getContentType() {
        switch (config.getContentType()) {
            case VPACK:
                return CONTENT_TYPE_VPACK;
            case JSON:
                return CONTENT_TYPE_APPLICATION_JSON;
            default:
                throw new IllegalArgumentException();
        }
    }

    private HttpClient createHttpClient(final ArangoRequest request, final int bodyLength) {
        return cookieStore.addCookies(client)
                .headers(headers -> {
                    headers.set(HttpHeaderNames.CONTENT_LENGTH, bodyLength);
                    headers.set(HttpHeaderNames.ACCEPT, getContentType());
                    addHeaders(request, headers);
                    if (bodyLength > 0) {
                        headers.set(HttpHeaderNames.CONTENT_TYPE, getContentType());
                    }
                });
    }

    private Mono<ArangoResponse> buildResponse(final HttpClientResponse resp, final ByteBufMono bytes) {
        return bytes
                .switchIfEmpty(Mono.just(Unpooled.EMPTY_BUFFER))
                .map(byteBuf -> {
                    byte[] buffer = IOUtils.getByteArray(byteBuf);
                    byteBuf.release();
                    return buffer;
                })
                .map(buffer -> ArangoResponse.builder()
                        .responseCode(resp.status().code())
                        .putAllMeta(resp.responseHeaders().entries().stream()
                                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
                        .body(buffer)
                        .build())
                .doOnNext(it -> {
                    LOGGER.debug("received response {}", it);
                    if (config.getResendCookies()) {
                        cookieStore.saveCookies(resp);
                    }
                });
    }

}
