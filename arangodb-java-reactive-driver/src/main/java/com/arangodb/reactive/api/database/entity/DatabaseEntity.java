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

package com.arangodb.reactive.api.database.entity;


import com.arangodb.reactive.api.entity.ReplicationFactor;
import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nullable;

/**
 * @author Michele Rastelli
 */
@GeneratePackagePrivateBuilder
@JsonDeserialize(builder = DatabaseEntityBuilder.class)
public interface DatabaseEntity {

    /**
     * @return the name of the database
     */
    String getName();

    /**
     * @return the id of the database
     */
    String getId();

    /**
     * @return the filesystem path of the database
     */
    @Nullable
    String getPath();

    /**
     * @return whether or not the database is the _system database
     */
    Boolean isSystem();

    /**
     * @return the default sharding method for collections created in this database
     */
    @Nullable
    Sharding getSharding();

    /**
     * @return the default replication factor for collections in this database
     */
    @Nullable
    ReplicationFactor getReplicationFactor();

    /**
     * @return the default write concern for collections in this database
     */
    @Nullable
    Integer getWriteConcern();

}
