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

package com.arangodb.reactive.communication;


import com.arangodb.reactive.connection.HostDescription;
import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;

/**
 * Represents an object carrying information about the host affinity of a sequence of client operations.
 *
 * @author Michele Rastelli
 */
@GeneratePackagePrivateBuilder
public interface Conversation {

    static Conversation of(HostDescription host, Level level) {
        return new ConversationBuilder()
                .host(host)
                .level(level)
                .build();
    }

    HostDescription getHost();

    Level getLevel();

    enum Level {

        /**
         * if the host is not available this causes ArangoCommunication to throw {@link com.arangodb.reactive.exceptions.HostNotAvailableException}
         */
        REQUIRED,

        /**
         * if the host is not available this causes ArangoCommunication to execute the request on another host
         */
        PREFERRED

    }

}
