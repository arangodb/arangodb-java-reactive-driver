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

package com.arangodb.reactive.entity.serde;

import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.function.Supplier;

/**
 * @author Michele Rastelli
 */
public enum ArangoDriverModule implements Supplier<Module> {
    INSTANCE;

    private final SimpleModule module;

    ArangoDriverModule() {
        module = new SimpleModule();
        module.addDeserializer(ReplicationFactor.class, VPackDeserializers.REPLICATION_FACTOR);
    }

    @Override
    public Module get() {
        return module;
    }

}
