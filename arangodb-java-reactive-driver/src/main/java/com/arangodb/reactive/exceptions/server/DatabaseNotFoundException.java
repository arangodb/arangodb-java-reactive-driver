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

package com.arangodb.reactive.exceptions.server;


import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;

/**
 * ArangoServerException having:
 * <code>
 * {
 * "code":404,
 * "error":true,
 * "errorMessage":"database not found",
 * "errorNum":1228
 * }
 * </code>
 *
 * @author Michele Rastelli
 */
@GeneratePackagePrivateBuilder
public abstract class DatabaseNotFoundException extends ArangoServerException {
    static final int ERROR_NUM = 1228;
}
