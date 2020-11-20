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

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import utils.TestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michele Rastelli
 */
public class ReusableNetwork implements Network {

    private static final String NAME_SUFFIX = "-arangodb-java-" + TestUtils.INSTANCE.getTestDockerImage();
    private static final Map<String, ReusableNetwork> instances = new HashMap<>();
    private final String name;
    private final String id;

    private ReusableNetwork(String name) {
        this.name = name;
        id = ensureNetwork();
    }

    synchronized public static ReusableNetwork of(String namePrefix) {
        String name = namePrefix + NAME_SUFFIX;
        ReusableNetwork instance = instances.get(name);
        if (instance == null) {
            instance = new ReusableNetwork(name);
            instances.put(name, instance);
        }
        return instance;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void close() {

    }

    public String getName() {
        return name;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        throw new UnsupportedOperationException();
    }

    private String ensureNetwork() {
        return DockerClientFactory.lazyClient()
                .listNetworksCmd()
                .withNameFilter(name)
                .exec()
                .stream()
                .findAny()
                .map(com.github.dockerjava.api.model.Network::getId)
                .orElseGet(this::createNetwork);
    }

    private String createNetwork() {
        return DockerClientFactory.lazyClient()
                .createNetworkCmd()
                .withName(name)
                .exec()
                .getId();
    }

}
