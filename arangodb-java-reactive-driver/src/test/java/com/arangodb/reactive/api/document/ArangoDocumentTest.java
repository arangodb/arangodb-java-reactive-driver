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
import com.arangodb.reactive.api.document.entity.DocumentEntity;
import com.arangodb.reactive.api.document.entity.OverwriteMode;
import com.arangodb.reactive.api.document.entity.SyncState;
import com.arangodb.reactive.api.document.options.DocumentCreateOptions;
import com.arangodb.reactive.api.document.options.DocumentReadOptions;
import com.arangodb.reactive.api.utils.ArangoApiTest;
import com.arangodb.reactive.api.utils.ArangoApiTestClass;
import com.arangodb.reactive.entity.serde.Id;
import com.arangodb.reactive.entity.serde.Key;
import com.arangodb.reactive.entity.serde.Rev;
import com.arangodb.reactive.exceptions.server.ArangoServerException;
import com.arangodb.reactive.exceptions.server.NotFoundException;
import com.arangodb.reactive.exceptions.server.NotModifiedException;
import com.arangodb.reactive.exceptions.server.PreconditionFailedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
@ArangoApiTestClass
public class ArangoDocumentTest {

    static class MyDoc {
        @Key
        public String key;

        @Id
        public String id;

        @Rev
        public String rev;

        public Map<String, Object> data;

        MyDoc() {
        }
    }

    @ArangoApiTest
    void createDocument(ArangoDocumentSync documentApi) {
        MyDoc doc = new MyDoc();
        doc.key = "key-" + UUID.randomUUID().toString();
        doc.data = Collections.singletonMap("key", "value");

        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(doc,
                DocumentCreateOptions.builder()
                        .returnNew(true)
                        .build());

        assertThat(created.getId()).isEqualTo(documentApi.collection().getName() + "/" + doc.key);
        assertThat(created.getKey()).isEqualTo(doc.key);
        assertThat(created.getRev()).isNotNull();
        assertThat(created.getOldRev()).isNull();
        assertThat(created.getNew()).isNotNull();
        assertThat(created.getOld()).isNull();
        assertThat(created.getSyncState()).isEqualTo(SyncState.ACCEPTED);

        MyDoc createdDoc = created.getNew();

        assertThat(createdDoc.key).isEqualTo(created.getKey());
        assertThat(createdDoc.id).isEqualTo(created.getId());
        assertThat(createdDoc.rev).isEqualTo(created.getRev());
        assertThat(createdDoc.data).isEqualTo(doc.data);
    }

    @ArangoApiTest
    void createDocumentOverwriteUpdate(ArangoDocumentSync documentApi) {
        MyDoc docA = new MyDoc();
        docA.key = "key-" + UUID.randomUUID().toString();
        docA.data = Map.of(
                "k1", "v1A",
                "k2", "v2A",
                "k3", "v3A"
        );
        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(docA);

        MyDoc docB = new MyDoc();
        docB.key = docA.key;
        docB.data = new HashMap<>();
        docB.data.put("k1", "v1B");
        docB.data.put("k3", null);
        docB.data.put("k4", "v4B");
        DocumentCreateEntity<MyDoc> updated = documentApi.createDocument(docB,
                DocumentCreateOptions.builder()
                        .waitForSync(true)
                        .returnNew(true)
                        .returnOld(true)
                        .overwriteMode(OverwriteMode.UPDATE)
                        .keepNull(false)
                        .mergeObjects(true)
                        .build());

        assertThat(updated.getId()).isEqualTo(documentApi.collection().getName() + "/" + docB.key);
        assertThat(updated.getKey()).isEqualTo(docB.key);
        assertThat(updated.getRev()).isNotNull();
        assertThat(updated.getOldRev()).isEqualTo(created.getRev());
        assertThat(updated.getNew()).isNotNull();
        assertThat(updated.getOld()).isNotNull();
        assertThat(updated.getSyncState()).isEqualTo(SyncState.CREATED);

        MyDoc oldDoc = updated.getOld();
        assertThat(oldDoc.key).isEqualTo(created.getKey());
        assertThat(oldDoc.id).isEqualTo(created.getId());
        assertThat(oldDoc.rev).isEqualTo(created.getRev());
        assertThat(oldDoc.data).isEqualTo(docA.data);

        MyDoc updatedDoc = updated.getNew();

        assertThat(updatedDoc.key).isEqualTo(updated.getKey());
        assertThat(updatedDoc.id).isEqualTo(updated.getId());
        assertThat(updatedDoc.rev).isEqualTo(updated.getRev());
        assertThat(updatedDoc.data).isEqualTo(Map.of(
                "k1", "v1B",
                "k2", "v2A",
                "k4", "v4B"
        ));
    }

    @ArangoApiTest
    void createDocumentOverwriteReplace(ArangoDocumentSync documentApi) {
        MyDoc docA = new MyDoc();
        docA.key = "key-" + UUID.randomUUID().toString();
        docA.data = Map.of("k1", "v1A");
        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(docA);

        MyDoc docB = new MyDoc();
        docB.key = docA.key;
        docB.data = Map.of("k1", "v1B");
        DocumentCreateEntity<MyDoc> updated = documentApi.createDocument(docB,
                DocumentCreateOptions.builder()
                        .waitForSync(true)
                        .returnNew(true)
                        .returnOld(true)
                        .overwriteMode(OverwriteMode.REPLACE)
                        .build());

        assertThat(updated.getId()).isEqualTo(documentApi.collection().getName() + "/" + docB.key);
        assertThat(updated.getKey()).isEqualTo(docB.key);
        assertThat(updated.getRev()).isNotNull();
        assertThat(updated.getOldRev()).isEqualTo(created.getRev());
        assertThat(updated.getNew()).isNotNull();
        assertThat(updated.getOld()).isNotNull();
        assertThat(updated.getSyncState()).isEqualTo(SyncState.CREATED);

        MyDoc oldDoc = updated.getOld();
        assertThat(oldDoc.key).isEqualTo(created.getKey());
        assertThat(oldDoc.id).isEqualTo(created.getId());
        assertThat(oldDoc.rev).isEqualTo(created.getRev());
        assertThat(oldDoc.data).isEqualTo(docA.data);

        MyDoc updatedDoc = updated.getNew();

        assertThat(updatedDoc.key).isEqualTo(updated.getKey());
        assertThat(updatedDoc.id).isEqualTo(updated.getId());
        assertThat(updatedDoc.rev).isEqualTo(updated.getRev());
        assertThat(updatedDoc.data).isEqualTo(docB.data);
    }

    @ArangoApiTest
    void createDocumentOverwriteIgnore(ArangoDocumentSync documentApi) {
        MyDoc docA = new MyDoc();
        docA.key = "key-" + UUID.randomUUID().toString();
        docA.data = Map.of("k1", "v1A");
        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(docA);

        MyDoc docB = new MyDoc();
        docB.key = docA.key;
        docB.data = Map.of("k1", "v1B");
        DocumentCreateEntity<MyDoc> updated = documentApi.createDocument(docB,
                DocumentCreateOptions.builder()
                        .waitForSync(false)
                        .returnNew(true)
                        .returnOld(true)
                        .overwriteMode(OverwriteMode.IGNORE)
                        .build());

        assertThat(updated.getId()).isEqualTo(documentApi.collection().getName() + "/" + docB.key);
        assertThat(updated.getKey()).isEqualTo(docB.key);
        assertThat(updated.getRev()).isNotNull();
        assertThat(updated.getOldRev()).isNull();
        assertThat(updated.getNew()).isNull();
        assertThat(updated.getOld()).isNull();
        assertThat(updated.getSyncState()).isEqualTo(SyncState.ACCEPTED);
    }

    @ArangoApiTest
    void getDocumentHead(ArangoDocumentSync documentApi) {
        MyDoc docA = new MyDoc();
        docA.key = "key-" + UUID.randomUUID().toString();
        docA.data = Map.of("k1", "v1A");
        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(docA);
        DocumentEntity head = documentApi.getDocumentHeader(docA.key);
        assertThat(head.getId()).isEqualTo(created.getId());
        assertThat(head.getKey()).isEqualTo(created.getKey());
        assertThat(head.getRev()).isEqualTo(created.getRev());
    }

    @ArangoApiTest
    void getDocumentHeadMatch(ArangoDocumentSync documentApi) {
        MyDoc docA = new MyDoc();
        docA.key = "key-" + UUID.randomUUID().toString();
        docA.data = Map.of("k1", "v1A");
        DocumentCreateEntity<MyDoc> created = documentApi.createDocument(docA);

        DocumentEntity matchingHead = documentApi.getDocumentHeader(docA.key, DocumentReadOptions.builder()
                .ifMatch(created.getRev()).build());
        assertThat(matchingHead.getId()).isEqualTo(created.getId());
        assertThat(matchingHead.getKey()).isEqualTo(created.getKey());
        assertThat(matchingHead.getRev()).isEqualTo(created.getRev());

        Throwable matchingHeadFail = catchThrowable(() -> documentApi
                .getDocumentHeader(docA.key, DocumentReadOptions.builder().ifMatch("nonMatchingEtag").build()));

        assertThat(matchingHeadFail).isNotNull();
        assertThat(matchingHeadFail).isInstanceOf(PreconditionFailedException.class);
        ArangoServerException matchingHeadEx = (ArangoServerException) matchingHeadFail;
        assertThat(matchingHeadEx.getResponseCode()).isEqualTo(412);
        assertThat(matchingHeadEx.getEntity()).isNotPresent();

        DocumentEntity noneMatchingHead = documentApi.getDocumentHeader(docA.key, DocumentReadOptions.builder()
                .ifNoneMatch("nonMatchingEtag").build());
        assertThat(noneMatchingHead.getId()).isEqualTo(created.getId());
        assertThat(noneMatchingHead.getKey()).isEqualTo(created.getKey());
        assertThat(noneMatchingHead.getRev()).isEqualTo(created.getRev());

        Throwable noneMatchingHeadFail = catchThrowable(() -> documentApi
                .getDocumentHeader(docA.key, DocumentReadOptions.builder().ifNoneMatch(created.getRev()).build()));

        assertThat(noneMatchingHeadFail).isNotNull();
        assertThat(noneMatchingHeadFail).isInstanceOf(NotModifiedException.class);
        ArangoServerException noneMatchingHeadEx = (ArangoServerException) noneMatchingHeadFail;
        assertThat(noneMatchingHeadEx.getResponseCode()).isEqualTo(304);
        assertThat(noneMatchingHeadEx.getEntity()).isNotPresent();

        Throwable nonExistingFail = catchThrowable(() -> documentApi.getDocumentHeader("nonExistingKey"));

        assertThat(nonExistingFail).isNotNull();
        assertThat(nonExistingFail).isInstanceOf(NotFoundException.class);
        ArangoServerException nonExistingEx = (ArangoServerException) nonExistingFail;
        assertThat(nonExistingEx.getResponseCode()).isEqualTo(404);
        assertThat(nonExistingEx.getEntity()).isNotPresent();
    }

}
