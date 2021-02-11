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
 * @see <a href="https://www.arangodb.com/docs/stable/http/document-working-with-documents.html#create-document">API
 * Documentation</a>
 */
@GenerateBuilder
public interface DocumentCreateOptions {

    static DocumentCreateOptionsBuilder builder() {
        return new DocumentCreateOptionsBuilder();
    }

    /**
     * @return Return additionally the complete new document under the attribute new in the result.
     */
    Optional<Boolean> getReturnNew();

    /**
     * @return Additionally return the complete old document under the attribute old in the result. Only available if
     * the {@code overwrite} option is used.
     */
    Optional<Boolean> getReturnOld();

}
