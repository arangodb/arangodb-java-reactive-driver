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

import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.ValueType;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Michele Rastelli
 */
public interface AuthenticationMethod {

    static AuthenticationMethod ofJwt(final String user, final String jwt) {
        return new JwtAuthenticationMethodBuilder()
                .user(user)
                .jwt(jwt)
                .build();
    }

    static AuthenticationMethod ofBasic(final String user, final String password) {
        return new BasicAuthenticationMethodBuilder()
                .user(user)
                .password(password)
                .build();
    }

    String getUser();

    String getHttpAuthorizationHeader();

    ByteBuf getVstAuthenticationMessage();

    /**
     * @see <a href="https://github.com/arangodb/velocystream#authentication">API</a>
     */
    @GeneratePackagePrivateBuilder
    abstract class JwtAuthenticationMethod implements AuthenticationMethod {

        abstract String getJwt();

        @Override
        public String getHttpAuthorizationHeader() {
            return "Bearer " + getJwt();
        }

        @Override
        public ByteBuf getVstAuthenticationMessage() {
            final VPackBuilder builder = new VPackBuilder();
            builder.add(ValueType.ARRAY);
            builder.add(1);
            builder.add(1000);
            builder.add("jwt");
            builder.add(getJwt());
            builder.close();
            return VPackUtils.extractBuffer(builder.slice());
        }

    }

    @GeneratePackagePrivateBuilder
    abstract class BasicAuthenticationMethod implements AuthenticationMethod {

        abstract String getPassword();

        @Override
        public String getHttpAuthorizationHeader() {
            final String plainAuth = getUser() + ":" + getPassword();
            final String encodedAuth = Base64.getEncoder().encodeToString(plainAuth.getBytes(StandardCharsets.UTF_8));
            return "Basic " + encodedAuth;
        }

        @Override
        public ByteBuf getVstAuthenticationMessage() {
            final VPackBuilder builder = new VPackBuilder();
            builder.add(ValueType.ARRAY);
            builder.add(1);
            builder.add(1000);
            builder.add("plain");
            builder.add(getUser());
            builder.add(getPassword());
            builder.close();
            return VPackUtils.extractBuffer(builder.slice());
        }

    }

}
