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
import com.arangodb.velocypack.VPackSlice;
import io.netty.buffer.ByteBuf;

/**
 * @author Mark Vollmary
 * @author Michele Rastelli
 */
final class ResponseConverter {

    private ResponseConverter() {
    }

    /**
     * @param buffer received VST buffer
     * @return ArangoDB response
     */
    static ArangoResponse decodeResponse(final byte[] buffer) {
        VPackSlice head = new VPackSlice(buffer);
        final int headSize = head.getByteSize();
        ByteBuf body = IOUtils.createBuffer(buffer.length - headSize);
        body.writeBytes(buffer, headSize, buffer.length - headSize);
        return VPackVstDeserializers.deserializeArangoResponse(head, body);
    }

}
