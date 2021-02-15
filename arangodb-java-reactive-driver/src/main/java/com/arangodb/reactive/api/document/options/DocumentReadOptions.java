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


package com.arangodb.reactive.api.document.options;

import com.arangodb.reactive.entity.GenerateBuilder;

import java.util.Optional;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
public interface DocumentReadOptions {

    String IF_NONE_MATCH = "If-None-Match";
    String IF_MATCH = "If-Match";

    static DocumentReadOptionsBuilder builder() {
        return new DocumentReadOptionsBuilder();
    }

    /**
     * @return If the “If-None-Match” header is given, then it must contain exactly one Etag. If the current document
     * revision is not equal to the specified Etag, an HTTP 200 response is returned. If the current document revision
     * is identical to the specified Etag, then an HTTP 304 is returned.
     */
    Optional<String> getIfNoneMatch();

    /**
     * @return If the “If-Match” header is given, then it must contain exactly one Etag. The document is returned, if it
     * has the same revision as the given Etag. Otherwise a HTTP 412 is returned.
     */
    Optional<String> getIfMatch();
}
