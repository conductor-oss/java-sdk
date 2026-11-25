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
package com.netflix.conductor.client.spring.compat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorker;
import com.netflix.conductor.sdk.workflow.task.InputParam;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Can a worker do its job? Task input is bound to a POJO and the returned POJO is
 * converted back to output data, both through the SDK's Jackson mapper.
 */
class Boot3WorkerTest {

    public static class Person {
        public String name;
    }

    public static class Greeting {
        public String greeting;
    }

    public static class Greeter {
        public Greeting greet(@InputParam("person") Person person) {
            Greeting result = new Greeting();
            result.greeting = "Hello " + person.name;
            return result;
        }
    }

    @Test
    void annotatedWorkerRoundTrip() throws Exception {
        AnnotatedWorker worker = new AnnotatedWorker(
                "greet", Greeter.class.getMethod("greet", Person.class), new Greeter());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("person", Map.of("name", "Ada")));

        TaskResult result = worker.execute(task);

        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(result.getOutputData()).containsEntry("greeting", "Hello Ada");
    }
}
