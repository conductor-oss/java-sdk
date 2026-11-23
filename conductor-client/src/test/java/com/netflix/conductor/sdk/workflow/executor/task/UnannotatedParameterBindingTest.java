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
package com.netflix.conductor.sdk.workflow.executor.task;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a {@code @WorkerTask} parameter without an annotation is bound when the worker is compiled
 * with {@code -parameters}, as Spring Boot applications are. The worker here is compiled that way
 * by the test, since this build is not.
 */
class UnannotatedParameterBindingTest {

    private static final String SOURCE = """
            package bound;

            import com.netflix.conductor.sdk.workflow.task.WorkerTask;

            public class Notifier {
                public static class Message {
                    public String message;
                    public String level;
                }

                @WorkerTask("notify")
                public String notify(Message message) {
                    return message.level + ": " + message.message;
                }

                @WorkerTask("count")
                public String count(java.util.Map<String, Object> data) {
                    return "keys " + new java.util.TreeSet<>(data.keySet());
                }

                @WorkerTask("tally")
                public String tally(@com.netflix.conductor.sdk.workflow.task.InputParam("values") java.util.Map<String, Object> counts) {
                    return "keys " + new java.util.TreeSet<>(counts.keySet());
                }

                @WorkerTask("greet")
                public String greet(String name, int times) {
                    return name + " x" + times;
                }
            }
            """;

    private static Object notifier;

    @BeforeAll
    static void compileWithParameterNames(@TempDir Path dir) throws Exception {
        Path source = Files.createDirectories(dir.resolve("src/bound")).resolve("Notifier.java");
        Files.writeString(source, SOURCE);
        Path classes = Files.createDirectories(dir.resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, "-parameters", "-d", classes.toString(),
                "-classpath", System.getProperty("java.class.path"), source.toString());
        assertEquals(0, status, "the worker did not compile");
        ClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, UnannotatedParameterBindingTest.class.getClassLoader());
        notifier = loader.loadClass("bound.Notifier").getDeclaredConstructor().newInstance();
    }

    private static Method method(String name) {
        for (Method m : notifier.getClass().getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new AssertionError(name);
    }

    private static Task task(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    /** A task as a native workflow schedules it: its entry names the lane of the code that called it. */
    private static Task nativeTask(Map<String, Object> input) {
        Task task = task(input);
        WorkflowTask entry = new WorkflowTask();
        entry.setMetadata(new HashMap<>(Map.of("lane", "main")));
        task.setWorkflowTask(entry);
        return task;
    }

    @Test
    void anObjectParameterStillBindsTheWholeInputWhenAFieldHasTheParametersName() {
        assertEquals(true, method("notify").getParameters()[0].isNamePresent(), "compiled with -parameters");
        AnnotatedWorker worker = new AnnotatedWorker("notify", method("notify"), notifier);

        // a workflow definition passes the object's fields, one of them named as the parameter is
        TaskResult result = worker.execute(task(Map.of("message", "hi", "level", "warn")));

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus(), result.getReasonForIncompletion());
        assertEquals("warn: hi", result.getOutputData().get("result"));
    }

    @Test
    void aNativeWorkflowsCallBindsEachParameterByItsName() {
        AnnotatedWorker worker = new AnnotatedWorker("notify", method("notify"), notifier);

        // a native workflow sends each argument under its parameter's name
        TaskResult result = worker.execute(nativeTask(Map.of("message", Map.of("message", "hi", "level", "warn"))));

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus(), result.getReasonForIncompletion());
        assertEquals("warn: hi", result.getOutputData().get("result"));
    }

    @Test
    void scalarParametersBindByNameFromAnyWorkflow() {
        AnnotatedWorker worker = new AnnotatedWorker("greet", method("greet"), notifier);

        for (Task task : List.of(task(Map.of("name", "ann", "times", 2)), nativeTask(Map.of("name", "ann", "times", 2)))) {
            TaskResult result = worker.execute(task);
            assertEquals(TaskResult.Status.COMPLETED, result.getStatus(), result.getReasonForIncompletion());
            assertEquals("ann x2", result.getOutputData().get("result"));
        }
    }

    @Test
    void aSingleMapParameterIsTheWholeInputOfAnyOtherWorkflow() {
        for (String name : List.of("count", "tally")) {
            AnnotatedWorker worker = new AnnotatedWorker(name, method(name), notifier);

            TaskResult result = worker.execute(task(Map.of("a", 1, "b", 2)));

            assertEquals("keys [a, b]", result.getOutputData().get("result"));
        }
    }

    @Test
    void aSingleMapParameterOfANativeWorkflowsCallIsTheMapItPassed() {
        AnnotatedWorker count = new AnnotatedWorker("count", method("count"), notifier);
        // a native workflow sends the map under its parameter's name, as it sends every argument
        assertEquals("keys [a, b]",
                count.execute(nativeTask(Map.of("data", Map.of("a", 1, "b", 2)))).getOutputData().get("result"));

        AnnotatedWorker tally = new AnnotatedWorker("tally", method("tally"), notifier);
        assertEquals("keys [a, b]",
                tally.execute(nativeTask(Map.of("values", Map.of("a", 1, "b", 2)))).getOutputData().get("result"));
    }
}
