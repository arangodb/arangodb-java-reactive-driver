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
import com.arangodb.reactive.api.arangodb.ArangoDBSync;
import com.arangodb.reactive.api.collection.ArangoCollection;
import com.arangodb.reactive.api.collection.ArangoCollectionSync;
import com.arangodb.reactive.api.database.ArangoDatabase;
import com.arangodb.reactive.api.database.ArangoDatabaseSync;
import com.arangodb.reactive.api.document.ArangoDocument;
import com.arangodb.reactive.api.document.ArangoDocumentSync;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Michele Rastelli
 */
public class ArangoApiParameterResolver implements ParameterResolver {

    private final TestContext testContext;
    private final ArangoDB testClient;

    public ArangoApiParameterResolver(TestContext testContext, ArangoDB testClient) {
        this.testContext = testContext;
        this.testClient = testClient;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return resolve(parameterContext.getParameter().getType(), extensionContext) != null;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Object o = resolve(parameterContext.getParameter().getType(), extensionContext);
        if (o != null) {
            return o;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + parameterContext.getParameter().getType());
        }
    }

    private Object resolve(Class<?> clazz, ExtensionContext extensionContext) {
        final String dbName = TestContext.getTestDbName(extensionContext);
        final String collectionName = TestContext.getTestCollectionName();

        final ArangoDB arangoDB = testClient;
        final ArangoDBSync arangoDBSync = testClient.sync();

        final ArangoDatabase db = arangoDB.db(dbName);
        final ArangoDatabaseSync dbSync = arangoDBSync.db(dbName);

        final ArangoCollection collection = db.collection(collectionName);
        final ArangoCollectionSync collectionSync = dbSync.collection(collectionName);

        final ArangoDocument document = collection.document();
        final ArangoDocumentSync documentSync = collectionSync.document();

        if (clazz == TestContext.class) {
            return testContext;
        } else if (clazz == ArangoDB.class) {
            return arangoDB;
        } else if (clazz == ArangoDBSync.class) {
            return arangoDBSync;
        } else if (clazz == ArangoDatabase.class) {
            return db;
        } else if (clazz == ArangoDatabaseSync.class) {
            return dbSync;
        } else if (clazz == ArangoCollection.class) {
            return collection;
        } else if (clazz == ArangoCollectionSync.class) {
            return collectionSync;
        } else if (clazz == ArangoDocument.class) {
            return document;
        } else if (clazz == ArangoDocumentSync.class) {
            return documentSync;
        } else {
            return null;
        }
    }
}
