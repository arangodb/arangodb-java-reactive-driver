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

package com.arangodb.reactive.api.document;


import com.arangodb.reactive.api.document.entity.DocumentCreateEntity;
import com.arangodb.reactive.api.document.options.DocumentCreateOptions;
import com.arangodb.reactive.api.utils.ArangoApiTest;
import com.arangodb.reactive.api.utils.ArangoApiTestClass;
import com.arangodb.reactive.entity.serde.Id;
import com.arangodb.reactive.entity.serde.Key;
import com.arangodb.reactive.entity.serde.Rev;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
@ArangoApiTestClass
public class ArangoDocumentTest {

    @ArangoApiTest
    void createDocument(ArangoDocumentSync documentApi) {
        DocumentCreateEntity<Map<String, String>> created = documentApi.createDocument(Collections.singletonMap("name", "test"));
        assertThat(created.getId()).isNotNull();
        assertThat(created.getKey()).isNotNull();
        assertThat(created.getRev()).isNotNull();
        assertThat(created.getNew()).isNull();
        assertThat(created.getOld()).isNull();
    }

    @ArangoApiTest
    void createDocumentFromUserClass(ArangoDocumentSync documentApi) {
        MyDoc doc = new MyDoc();
        doc.key = "key-" + UUID.randomUUID().toString();
        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(doc,
                DocumentCreateOptions.builder()
                        .returnNew(true)
                        .build());
        assertThat(created.getId()).isEqualTo(documentApi.collection().getName() + "/" + doc.key);
        assertThat(created.getKey()).isEqualTo(doc.key);
        assertThat(created.getRev()).isNotNull();
        assertThat(created.getNew()).isNotNull();
        MyDoc createdDoc = created.getNew();
        assertThat(createdDoc.key).isEqualTo(created.getKey());
        assertThat(createdDoc.id).isEqualTo(created.getId());
        assertThat(createdDoc.rev).isEqualTo(created.getRev());
        assertThat(created.getOld()).isNull();
    }


    static class MyDoc {
        @Key
        String key;

        @Id
        String id;

        @Rev
        String rev;

        MyDoc() {
        }
    }

}
