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

import com.arangodb.reactive.api.database.DatabaseApi;
import com.arangodb.reactive.api.reactive.impl.ArangoDBImpl;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Michele Rastelli
 */
public class ArangoApiTestClassExtension implements BeforeAllCallback, AfterAllCallback {

    private final static List<TestContext> contexts = TestContextProvider.INSTANCE.get();

    @Override
    public void afterAll(ExtensionContext context) {
        String dbName = context.getRequiredTestClass().getSimpleName();
        doForeachTopology(dbApi -> dbApi.dropDatabase(dbName));
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        String dbName = context.getRequiredTestClass().getSimpleName();
        doForeachTopology(dbApi -> dbApi.createDatabase(dbName));
    }

    private void doForeachTopology(Function<DatabaseApi, Mono<?>> action) {
        Flux
                .fromStream(
                        contexts.stream()
                                .collect(Collectors.groupingBy(it -> it.getConfig().getTopology()))
                                .values()
                                .stream()
                                .map(ctxList -> new ArangoDBImpl(ctxList.get(0).getConfig()))
                )
                .flatMap(testClient -> action.apply(testClient.db()))
                .then().block();
    }
}
