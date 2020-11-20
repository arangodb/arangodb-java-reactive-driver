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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static com.arangodb.reactive.ArangoDefaults.HEADER_SIZE;

/**
 * @author Michele Rastelli
 */
final class VstReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(VstReceiver.class);

    private final ChunkStore chunkStore;
    private final ByteBuf chunkHeaderBuffer;
    private final ByteBuf chunkContentBuffer;
    private Chunk chunk;

    VstReceiver(final BiConsumer<Long, ArangoResponse> callback) {
        chunkStore = new ChunkStore(callback);
        chunkHeaderBuffer = IOUtils.createBuffer();
        chunkContentBuffer = IOUtils.createBuffer();
    }

    void clear() {
        VstConnection.assertCorrectThread();
        LOGGER.debug("clear()");

        chunkStore.clear();
    }

    void shutDown() {
        VstConnection.assertCorrectThread();
        LOGGER.debug("shutDown()");

        clear();
        chunkHeaderBuffer.release();
        chunkContentBuffer.release();
    }

    void handleByteBuf(final ByteBuf bbIn) {
        VstConnection.assertCorrectThread();

        while (bbIn.readableBytes() > 0) {

            // new chunk
            if (chunk == null) {
                int missingHeaderBytes = HEADER_SIZE - chunkHeaderBuffer.readableBytes();
                readBytes(bbIn, chunkHeaderBuffer, missingHeaderBytes);
                if (chunkHeaderBuffer.readableBytes() == HEADER_SIZE) {
                    readHeader();
                }
            }

            if (chunk != null) {
                int missingContentBytes = chunk.getContentLength() - chunkContentBuffer.readableBytes();
                readBytes(bbIn, chunkContentBuffer, missingContentBytes);

                // chunkContent completely received
                if (chunkContentBuffer.readableBytes() == chunk.getContentLength()) {
                    readContent();
                }
            }

        }

        bbIn.release();
    }

    private void readBytes(final ByteBuf bbIn, final ByteBuf out, final int len) {
        int bytesToRead = Integer.min(len, bbIn.readableBytes());
        out.ensureWritable(bytesToRead);
        bbIn.readBytes(out, bytesToRead);
    }

    private void readHeader() {
        final int chunkLength = chunkHeaderBuffer.readIntLE();
        final int chunkX = chunkHeaderBuffer.readIntLE();

        final long messageId = chunkHeaderBuffer.readLongLE();
        final long messageLength = chunkHeaderBuffer.readLongLE();
        final int contentLength = chunkLength - HEADER_SIZE;

        chunk = new Chunk(messageId, chunkX, messageLength, 0, contentLength);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Received chunk %s:%s from message %s", chunk.getChunk(), chunk.isFirstChunk() ? 1 : 0, chunk.getMessageId()));
        }
    }

    private void readContent() {
        chunkStore.storeChunk(chunk, chunkContentBuffer);
        chunkHeaderBuffer.clear();
        chunkContentBuffer.clear();
        chunk = null;
    }

}
