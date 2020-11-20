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

import com.arangodb.reactive.entity.model.ErrorEntity;
import com.arangodb.reactive.exceptions.ArangoException;

/**
 * @author Michele Rastelli
 */
public abstract class ArangoServerException extends ArangoException {

    public static ArangoServerException of(final int responseCode, final ErrorEntity errorEntity) {
        switch (errorEntity.getErrorNum()) {
            case CollectionOrViewNotFoundException.ERROR_NUM:
                return new CollectionOrViewNotFoundExceptionBuilder()
                        .responseCode(responseCode)
                        .entity(errorEntity)
                        .build();
            default:
                return new GenericArangoServerExceptionBuilder()
                        .responseCode(responseCode)
                        .entity(errorEntity)
                        .build();
        }
    }

    public abstract int getResponseCode();

    public abstract ErrorEntity getEntity();

}
