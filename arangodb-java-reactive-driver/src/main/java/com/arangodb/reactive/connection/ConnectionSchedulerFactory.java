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

package com.arangodb.reactive.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Michele Rastelli
 */
public final class ConnectionSchedulerFactory {

    public static final String THREAD_PREFIX = "arango-connection";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionSchedulerFactory.class);

    private final int maxThreads;
    private final List<Scheduler> schedulers;
    private final AtomicInteger cursor;

    public ConnectionSchedulerFactory(final int maxAllowedThreads) {
        maxThreads = maxAllowedThreads;
        schedulers = new ArrayList<>();
        cursor = new AtomicInteger();
    }

    public synchronized Scheduler getScheduler() {
        int position = cursor.getAndIncrement();
        if (position < maxThreads) {
            LOGGER.debug("Creating single thread connection scheduler #{}", position);
            schedulers.add(Schedulers.newSingle(THREAD_PREFIX));
        }
        return schedulers.get(position % maxThreads);
    }

    public void close() {
        schedulers.forEach(Scheduler::dispose);
        schedulers.clear();
    }

}
