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

import com.arangodb.reactive.api.database.options.DatabaseCreateOptions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author Michele Rastelli
 */
public class ArangoApiTestClassExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) {
        String dbName = context.getRequiredTestClass().getSimpleName();
        TestContextProvider.doForeachDeployment(arangoDB -> arangoDB.db(dbName).drop());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        String dbName = context.getRequiredTestClass().getSimpleName();
        TestContextProvider.doForeachDeployment(arangoDB -> arangoDB.createDatabase(
                DatabaseCreateOptions.builder()
                .name(dbName)
                        .addUsers(DatabaseCreateOptions.DatabaseUser.builder()
                                .username(TestContext.USER_NAME)
                                .build())
                        .build()));
    }

}
