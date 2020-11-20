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

import org.immutables.value.Value;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @author Michele Rastelli
 */
@Value.Immutable(builder = false)
public interface TypedArguments<T> extends Arguments {

    static <T> TypedArguments<T> of(TestContext testContext, T testClient) {
        return ImmutableTypedArguments.of(testContext, testClient);
    }

    @Value.Parameter(order = 1)
    TestContext getTestContext();

    @Value.Parameter(order = 2)
    T getTestClient();

    @Override
    default Object[] get() {
        return new Object[]{
                getTestContext(),
                getTestClient()
        };
    }

}
