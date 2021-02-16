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

package com.arangodb.reactive.api.document.options;


import com.arangodb.reactive.api.document.entity.OverwriteMode;
import com.arangodb.reactive.entity.GenerateBuilder;

import java.util.Optional;

/**
 * @author Michele Rastelli
 * @see <a href="https://www.arangodb.com/docs/stable/http/document-working-with-documents.html#create-document">API
 * Documentation</a>
 */
@GenerateBuilder
public interface DocumentCreateOptions {

    String WAIT_FOR_SYNC = "waitForSync";
    String RETURN_NEW = "returnNew";
    String RETURN_OLD = "returnOld";
    String OVERWRITE = "overwrite";
    String OVERWRITE_MODE = "overwriteMode";
    String KEEP_NULL = "keepNull";
    String MERGE_OBJECTS = "mergeObjects";

    static DocumentCreateOptionsBuilder builder() {
        return new DocumentCreateOptionsBuilder();
    }

    /**
     * @return Wait until document has been synced to disk.
     */
    Optional<Boolean> getWaitForSync();

    /**
     * @return Return additionally the complete new document under the attribute new in the result.
     */
    Optional<Boolean> getReturnNew();

    /**
     * @return Additionally return the complete old document under the attribute old in the result. Only available if
     * the {@code overwrite} option is used.
     */
    Optional<Boolean> getReturnOld();

    /**
     * @return If set to true, the insert becomes a replace-insert. If a document with the same _key already exists the
     * new document is not rejected with unique constraint violated but will replace the old document. Note that
     * operations with overwrite parameter require a _key attribute in the request payload, therefore they can only be
     * performed on collections sharded by _key.
     */
    Optional<Boolean> getOverwrite();

    /**
     * @return This option supersedes overwrite
     *
     * @since ArangoDB 3.7
     */
    Optional<OverwriteMode> getOverwriteMode();

    /**
     * @return If the intention is to delete existing attributes with the update-insert command, the URL query parameter
     * keepNull can be used with a value of false. This will modify the behavior of the patch command to remove any
     * attributes from the existing document that are contained in the patch document with an attribute value of null.
     * This option controls the update-insert behavior only.
     */
    Optional<Boolean> getKeepNull();

    /**
     * @return Controls whether objects (not arrays) will be merged if present in both the existing and the
     * update-insert document. If set to false, the value in the patch document will overwrite the existing document’s
     * value. If set to true, objects will be merged. The default is true. This option controls the update-insert
     * behavior only.
     */
    Optional<Boolean> getMergeObjects();

}
