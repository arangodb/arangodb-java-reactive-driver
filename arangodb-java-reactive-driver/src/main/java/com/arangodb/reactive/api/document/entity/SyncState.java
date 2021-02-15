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

package com.arangodb.reactive.api.document.entity;


/**
 * @author Michele Rastelli
 */
public enum SyncState {

    /**
     * if the documents were created successfully and waitForSync was true
     */
    CREATED(201),

    /**
     * if the documents were created successfully and waitForSync was false
     */
    ACCEPTED(202);

    private final int value;

    public static SyncState of(final int value) {
        for (SyncState e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown value for SyncState: " + value);
    }

    SyncState(final int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }

}
