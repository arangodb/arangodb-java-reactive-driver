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


package com.arangodb.reactive.api.arangodb.impl;

import com.arangodb.reactive.api.arangodb.ArangoDB;
import com.arangodb.reactive.api.arangodb.ArangoDBSync;
import com.arangodb.reactive.api.database.DatabaseApi;
import com.arangodb.reactive.api.database.impl.DatabaseApiImpl;
import com.arangodb.reactive.api.reactive.impl.ArangoClientImpl;
import com.arangodb.reactive.communication.CommunicationConfig;
import reactor.core.publisher.Mono;

/**
 * @author Michele Rastelli
 */
public final class ArangoDBImpl extends ArangoClientImpl implements ArangoDB {

    public ArangoDBImpl(final CommunicationConfig config) {
        super(config);
    }

    @Override
    public ArangoDBSync sync() {
        return new ArangoDBSyncImpl(this);
    }

    @Override
    public DatabaseApi db() {
        return db("_system");
    }

    @Override
    public DatabaseApi db(final String name) {
        return new DatabaseApiImpl(this, name);
    }

    @Override
    public Mono<Void> shutdown() {
        return getCommunication().close();
    }

}
