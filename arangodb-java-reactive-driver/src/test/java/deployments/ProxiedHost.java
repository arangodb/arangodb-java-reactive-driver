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


package deployments;

import com.arangodb.reactive.connection.HostDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ToxiproxyContainer;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * @author Michele Rastelli
 */
@Value.Immutable
public abstract class ProxiedHost {

    private static final Logger log = LoggerFactory.getLogger(ProxiedHost.class);

    static ImmutableProxiedHost.Builder builder() {
        return ImmutableProxiedHost.builder();
    }

    abstract ToxiproxyContainer getToxiproxy();

    abstract String getProxiedHost();

    abstract int getProxiedPort();

    public HostDescription getHostDescription() {
        return HostDescription.of(getProxy().getContainerIpAddress(), getProxy().getProxyPort());
    }

    public ToxiproxyContainer.ContainerProxy getProxy() {
        return getToxiproxy().getProxy(getProxiedHost(), getProxiedPort());
    }

    public void enableProxy() {
        log.debug("enableProxy()");
        setProxyEnabled(true);
        log.debug("... enableProxy() done");
    }

    public void disableProxy() {
        log.debug("disableProxy()");
        setProxyEnabled(false);
        log.debug("... disableProxy() done");
    }

    /**
     * Bringing a service down is not technically a toxic in the implementation of Toxiproxy. This is done by POSTing
     * to /proxies/{proxy} and setting the enabled field to false.
     *
     * @param value value to set
     * @see <a href="https://github.com/Shopify/toxiproxy#down">
     */
    private void setProxyEnabled(boolean value) {
        String request = new ObjectMapper().createObjectNode().put("enabled", value).toString();
        String response = HttpClient.create()
                .post()
                .uri("http://" + getToxiproxy().getContainerIpAddress() + ":" + getToxiproxy().getMappedPort(8474)
                        + "/proxies/" + getProxiedHost() + ":" + getProxiedPort())
                .send(Mono.just(Unpooled.wrappedBuffer(request.getBytes())))
                .responseContent()
                .asString()
                .blockFirst();

        log.debug(response);
    }

}
