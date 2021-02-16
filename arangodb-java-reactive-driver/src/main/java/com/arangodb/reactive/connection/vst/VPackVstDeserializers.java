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
import com.arangodb.reactive.connection.ArangoResponseBuilder;
import com.arangodb.reactive.connection.IOUtils;
import com.arangodb.velocypack.VPackSlice;
import io.netty.buffer.ByteBuf;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * @author Michele Rastelli
 */
final class VPackVstDeserializers {

    private VPackVstDeserializers() {
    }

    static ArangoResponse deserializeArangoResponse(final VPackSlice vpack, final ByteBuf body) {
        byte[] bodyBytes = IOUtils.getByteArray(body);
        body.release();

        ArangoResponseBuilder builder = ArangoResponse.builder()
                .body(bodyBytes)
                .version(vpack.get(0).getAsInt())
                .type(vpack.get(1).getAsInt())
                .responseCode(vpack.get(2).getAsInt());

        if (vpack.size() > 3) {
            Iterator<Map.Entry<String, VPackSlice>> metaIterator = vpack.get(3).objectIterator();
            while (metaIterator.hasNext()) {
                Map.Entry<String, VPackSlice> meta = metaIterator.next();
                builder.putMeta(meta.getKey().toLowerCase(Locale.ROOT), meta.getValue().getAsString());
            }
        }

        return builder.build();
    }

}
