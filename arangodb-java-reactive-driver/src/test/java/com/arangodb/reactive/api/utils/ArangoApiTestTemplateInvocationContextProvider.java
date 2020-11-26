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

import com.arangodb.reactive.api.arangodb.ArangoDB;
import com.arangodb.reactive.api.arangodb.impl.ArangoDBImpl;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Michele Rastelli
 */
public class ArangoApiTestTemplateInvocationContextProvider implements TestTemplateInvocationContextProvider {

    private final static List<TestContext> contexts = TestContextProvider.INSTANCE.get();
    private final static List<ArangoDB> testClients = contexts.stream()
            .map(it -> new ArangoDBImpl(it.getConfig()))
            .collect(Collectors.toList());

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return IntStream.range(0, contexts.size())
                .mapToObj(i -> invocationContext(contexts.get(i), testClients.get(i)));
    }

    private TestTemplateInvocationContext invocationContext(TestContext ctx, ArangoDB testClient) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return ctx.toString();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return Collections.singletonList(new ArangoApiParameterResolver(ctx, testClient));
            }
        };
    }
}
