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

package com.arangodb.reactive.connection.vst;

import com.arangodb.reactive.connection.ArangoResponse;
import com.arangodb.reactive.connection.IOUtils;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Mark Vollmary
 * @author Michele Rastelli
 */
final class ChunkStore {

    private final Map<Long, ByteBuf> data;
    private final BiConsumer<Long, ArangoResponse> callback;

    ChunkStore(final BiConsumer<Long, ArangoResponse> responseCallback) {
        data = new HashMap<>();
        callback = responseCallback;
    }

    void storeChunk(final Chunk chunk, final ByteBuf inBuf) {
        final long messageId = chunk.getMessageId();
        ByteBuf chunkBuffer = data.get(messageId);
        if (chunkBuffer == null) {
            final int length = chunk.getChunk() > 1 ? (int) chunk.getMessageLength() : chunk.getContentLength();
            chunkBuffer = IOUtils.createBuffer(length, length);
            data.put(messageId, chunkBuffer);
        }

        chunkBuffer.writeBytes(inBuf);
        checkCompleteness(messageId, chunkBuffer);
    }

    private void checkCompleteness(final long messageId, final ByteBuf chunkBuffer) {
        if (chunkBuffer.readableBytes() == chunkBuffer.capacity()) {
            byte[] bytes = new byte[chunkBuffer.readableBytes()];
            chunkBuffer.readBytes(bytes);
            chunkBuffer.release();
            callback.accept(messageId, ResponseConverter.decodeResponse(bytes));
            data.remove(messageId);
        }
    }

    void clear() {
        data.values().forEach(ReferenceCounted::release);
        data.clear();
    }

}
