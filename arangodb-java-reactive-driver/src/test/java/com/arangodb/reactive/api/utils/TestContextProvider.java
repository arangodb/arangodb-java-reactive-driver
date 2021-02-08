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

package com.arangodb.reactive.api.utils;

import com.arangodb.reactive.api.arangodb.ArangoDBSync;
import com.arangodb.reactive.api.arangodb.impl.ArangoDBImpl;
import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import com.arangodb.reactive.exceptions.ArangoException;
import deployments.ContainerDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.TestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * @author Michele Rastelli
 */
public enum TestContextProvider implements Supplier<List<TestContext>> {
    INSTANCE;

    private final Logger log = LoggerFactory.getLogger(TestContextProvider.class);

    private final List<TestContext> contexts;

    TestContextProvider() {
        try {
            long start = new Date().getTime();

            List<ContainerDeployment> deployments;

            if (TestUtils.INSTANCE.isUseProvidedDeployment()) {
                log.info("Using provided database deployment");
                deployments = Collections.singletonList(ContainerDeployment.ofProvidedDeployment());
            } else {
                log.info("No database deployment provided, starting containers...");
                deployments = Arrays.asList(
                        ContainerDeployment.ofReusableSingleServer(),
                        ContainerDeployment.ofReusableActiveFailover(),
                        ContainerDeployment.ofReusableCluster()
                );

                List<Thread> startingTasks = deployments.stream()
                        .map(it -> new Thread(it::start))
                        .collect(Collectors.toList());

                startingTasks.forEach(Thread::start);
                for (Thread t : startingTasks) {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            contexts = deployments.stream()
                    .flatMap(TestContext::createContexts)
                    .collect(Collectors.toList());

            // FIXME: is this necessary? use the test suite database for this purpose
            // create user and userDB
            doForeachDeployment(arangoDB -> {
                        try {
                            arangoDB.db(TestContext.USER_DB).info();
                        } catch (ArangoException e) {
                            arangoDB.createDatabase(DatabaseCreateOptions.builder()
                                    .name(TestContext.USER_DB)
                                    .addUsers(DatabaseCreateOptions.DatabaseUser.builder()
                                            .username(TestContext.USER_NAME)
                                            .passwd(TestContext.USER_PASSWD)
                                            .build())
                                    .build());
                        }
                    }

            );

            long end = new Date().getTime();
            log.info("TestContextProvider initialized in [ms]: {}", end - start);
        } catch (Exception e) {
            log.error("Exception in TestUtils initializer", e);
            System.exit(1);
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public List<TestContext> get() {
        return contexts;
    }

    public void doForeachDeployment(Consumer<ArangoDBSync> action) {
        contexts.stream()
                .collect(Collectors.groupingBy(TestContext::getDeployment))
                .values()
                .stream()
                .map(ctxList -> new ArangoDBImpl(ctxList.get(0).getConfig()).sync())
                .forEach(action);
    }

}
