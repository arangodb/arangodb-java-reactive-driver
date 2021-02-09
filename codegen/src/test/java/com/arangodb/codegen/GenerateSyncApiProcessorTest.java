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

package com.arangodb.codegen;


import api.NestedApi;
import api.TestApi;
import api.TestApiImpl;
import com.sun.tools.javac.Main;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
class GenerateSyncApiProcessorTest {
    private final static String GENERATED_DIR = "generated";
    private final static String SOURCE_DIR = GENERATED_DIR + "/source";
    private final static String COMPILED_DIR = GENERATED_DIR + "/compiled";

    private final ClassLoader cl;

    GenerateSyncApiProcessorTest() throws MalformedURLException {
        cl = new URLClassLoader(new URL[]{new File(COMPILED_DIR).toURI().toURL()});
    }

    @BeforeAll
    static void generateAndCompile() throws IOException {
        cleanup();

        // generate sources
        Main.compile(Stream
                .concat(
                        Stream.of("-d", SOURCE_DIR, "-proc:only",
                                "-processor", GenerateSyncApiProcessor.class.getCanonicalName()),
                        Files.walk(Paths.get("src/test/java/api/"))
                                .filter(i -> i.getFileName().toString().endsWith(".java"))
                                .map(Path::toString)
                )
                .toArray(String[]::new)
        );

        // compile sources
        Main.compile(Stream
                .concat(
                        Stream.of("-d", COMPILED_DIR),
                        Files.walk(Paths.get(SOURCE_DIR))
                                .filter(i -> i.getFileName().toString().endsWith(".java"))
                                .map(Path::toString)
                )
                .toArray(String[]::new)
        );
    }

    @AfterAll
    static void cleanup() throws IOException {
        // clean generated directory
        FileUtils.deleteDirectory(new File(GENERATED_DIR));
    }

    private static Object[] createStringArguments(Method m) {
        return IntStream.range(0, m.getParameterCount())
                .mapToObj(String::valueOf)
                .toArray();
    }

    @Test
    void loadGeneratedClasses() throws Exception {
        loadSyncClass(NestedApi.class);
        loadSyncClass(TestApi.class);
        loadSyncImplClass(NestedApi.class);
        Class<?> testApiSyncImplClass = loadSyncImplClass(TestApi.class);

        var reactiveInstance = new TestApiImpl();
        var syncInstance = testApiSyncImplClass
                .getConstructor(TestApi.class)
                .newInstance(reactiveInstance);

        for (Method reactiveMethod : TestApiImpl.class.getDeclaredMethods()) {
            Method syncMethod = testApiSyncImplClass.getDeclaredMethod(reactiveMethod.getName(), reactiveMethod.getParameterTypes());

            Object[] args = createStringArguments(syncMethod);

            var reactiveResult = reactiveMethod.invoke(reactiveInstance, args);
            var syncResult = syncMethod.invoke(syncInstance, args);

            Object expectedResult;
            if (reactiveResult instanceof Mono) {
                expectedResult = ((Mono<?>) reactiveResult).block();
            } else if (reactiveResult instanceof Flux) {
                expectedResult = ((Flux<?>) reactiveResult).collectList().block();
            } else {
                expectedResult = reactiveResult;
            }

            if (reactiveMethod.getName().equals("nested") || reactiveMethod.getName().equals("nestedAsync")) {
                assertThat(
                        syncResult.getClass().getMethod("name").invoke(syncResult)
                ).isEqualTo(
                        expectedResult.getClass().getMethod("name").invoke(expectedResult)
                );
            } else {
                assertThat(syncResult).isEqualTo(expectedResult);
            }
        }

    }

    private Class<?> loadSyncClass(Class<?> clazz) throws ClassNotFoundException {
        String testApiSyncClassName = clazz.getCanonicalName() + "Sync";
        return cl.loadClass(testApiSyncClassName);
    }

    private Class<?> loadSyncImplClass(Class<?> clazz) throws ClassNotFoundException {
        String packageName = clazz.getPackageName();
        String testApiSyncImplClassName = packageName + ".impl." + clazz.getSimpleName() + "SyncImpl";
        return cl.loadClass(testApiSyncImplClassName);
    }

}
