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


import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Michele Rastelli
 */
final class CommunicationUtils {

    private CommunicationUtils() {
    }

    /**
     * @throws NoSuchElementException if the collection is empty
     */
    static <T> T getRandomItem(final Collection<T> collection) {
        if (collection.isEmpty()) {
            throw new NoSuchElementException();
        }
        int index = ThreadLocalRandom.current().nextInt(collection.size());
        Iterator<T> iterator = collection.iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        return iterator.next();
    }

}
