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


package com.arangodb.reactive.api.sync.impl;

import com.arangodb.reactive.api.reactive.ArangoDB;
import com.arangodb.reactive.api.reactive.impl.ArangoDatabaseImpl;
import com.arangodb.reactive.api.sync.ArangoDBSync;
import com.arangodb.reactive.api.sync.ArangoDatabaseSync;

/**
 * @author Michele Rastelli
 */
public final class ArangoDBSyncImpl extends ClientSyncImpl<ArangoDB> implements ArangoDBSync {


    public ArangoDBSyncImpl(final ArangoDB arangoDB) {
        super(arangoDB);
    }

    @Override
    public ArangoDatabaseSync db(final String name) {
        return new ArangoDatabaseSyncImpl(new ArangoDatabaseImpl(reactive(), name));
    }

}
