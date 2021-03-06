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

package com.arangodb.reactive.entity.model;


import com.arangodb.reactive.entity.GeneratePackagePrivateBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Michele Rastelli
 */
@GeneratePackagePrivateBuilder
@JsonDeserialize(builder = VersionBuilder.class)
public interface Version {

    String getServer();

    String getLicense();

    String getVersion();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, String> getDetails();

}
