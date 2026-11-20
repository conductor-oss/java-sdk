/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.conductoross.conductor.sdk.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

/**
 * Checks that the current code still matches a recorded execution: re-runs the workflow method
 * against the execution's tasks, exactly as a worker does when it processes a continuation, and
 * fails on any divergence. Record executions with {@code GET /api/workflow/{id}?includeTasks=true}
 * (or {@code conductor workflow get <id>}) and keep a few in the test resources, so a change that
 * would break executions in flight fails in CI.
 */
public final class DurableReplay {

    private DurableReplay() {}

    public static void assertCompatible(Class<?> workflowClass, Path recordedExecution) {
        assertCompatible(workflowClass, null, recordedExecution);
    }

    @SuppressWarnings("unchecked")
    public static void assertCompatible(Class<?> workflowClass, Supplier<?> factory, Path recordedExecution) {
        Map<String, Object> json;
        try {
            json = Json.MAPPER.readValue(Files.readString(recordedExecution), Map.class);
        } catch (IOException e) {
            throw new AssertionError("cannot read recorded execution " + recordedExecution + ": " + e.getMessage(), e);
        }
        Execution exec = new Execution(json);
        String name = String.valueOf(exec.definition.get("name"));
        WorkflowSpec spec =
                WorkflowSpec.of(workflowClass).stream()
                        .filter(s -> s.name.equals(name))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(workflowClass.getSimpleName() + " has no @Workflow named " + name));
        Object instance = factory != null ? factory.get() : spec.newInstance();
        try {
            new Replayer(spec, instance, exec).process(null, 0);
        } catch (NonDeterminismException e) {
            throw new AssertionError("the code no longer matches execution " + exec.workflowId + ": " + e.getMessage(), e);
        }
    }
}
