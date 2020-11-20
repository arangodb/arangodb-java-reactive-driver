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

package com.arangodb.reactive.api.database.options;

import com.arangodb.reactive.api.database.entity.Sharding;
import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.entity.GenerateBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author Michele Rastelli
 */
@GenerateBuilder
public interface DatabaseCreateOptions {

    static DatabaseCreateOptionsBuilder builder() {
        return new DatabaseCreateOptionsBuilder();
    }

    /**
     * @return a valid database name
     */
    String getName();

    /**
     * @return {@link Options}
     * @since ArangoDB 3.6
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Options getOptions();

    /**
     * A list of users to initially create for the new database.
     * User information will not be changed for users that already exist.
     * If users is not specified or does not contain any users, a default user
     * root will be created with an empty string password. This ensures that the
     * new database will be accessible after it is created.
     *
     * @return {@link DatabaseUser}
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<DatabaseUser> getUsers();

    @GenerateBuilder
    interface Options {

        static OptionsBuilder builder() {
            return new OptionsBuilder();
        }

        /**
         * @return Default replication factor for new collections created in this database. Special values include "satellite",
         * which will replicate the collection to every DB-server, and 1, which disables replication. (cluster only)
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ReplicationFactor getReplicationFactor();

        /**
         * @return Default write concern for new collections created in this database. It determines how many copies of each
         * shard are required to be in sync on the different DBServers. If there are less then these many copies in the
         * cluster a shard will refuse to write. Writes to shards with enough up-to-date copies will succeed at the same
         * time however. The value of writeConcern can not be larger than replicationFactor. (cluster only)
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer getWriteConcern();

        /**
         * @return The sharding method to use for new collections in this database.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Sharding getSharding();

    }

    @GenerateBuilder
    interface DatabaseUser {

        static DatabaseUserBuilder builder() {
            return new DatabaseUserBuilder();
        }

        /**
         * @return Login name of the user to be created.
         */
        String getUsername();

        /**
         * @return The user password as a string.
         * @defaultValue <code>""</code>
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String getPasswd();

        /**
         * @return A flag indicating whether the user account should be activated or not.
         * If set to false, the user won't be able to log into the database.
         * @defaultValue <code>true</code>
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean isActive();

        /**
         * @return A JSON object with extra user information. It is used by the web interface
         * to store graph viewer settings and saved queries. Should not be set or
         * modified by end users, as custom attributes will not be preserved.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> getExtra();
    }

}
