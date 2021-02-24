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

package com.arangodb.reactive.api.utils;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Michele Rastelli
 */
public class RootOnlyExecutionCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getTestMethod().get().isAnnotationPresent(SystemDBOnly.class)) {
            TestTemplateInvocationContext ic = getInvocationContext(context);
            if (ic instanceof ArangoApiTestTemplateInvocationContextProvider.InvocationContext) {
                String adminDB = ((ArangoApiTestTemplateInvocationContextProvider.InvocationContext) ic).getAdminDB();
                if (!"_system".equals(adminDB)) {
                    return ConditionEvaluationResult.disabled("@SystemDBOnly: test disabled for adminDB: " + adminDB);
                }
            }
        }
        return ConditionEvaluationResult.enabled("No @SystemDBOnly annotation found, test enabled.");
    }

    private TestTemplateInvocationContext getInvocationContext(ExtensionContext context) {
        Method method = ReflectionUtils.findMethod(context.getClass(), "getTestDescriptor").get();
        Object descriptor = ReflectionUtils.invokeMethod(method, context);
        try {
            Field templateField = descriptor.getClass().getDeclaredField("invocationContext");
            templateField.setAccessible(true);
            TestTemplateInvocationContext ic = (TestTemplateInvocationContext) templateField.get(descriptor);
            return ic;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
