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
package examples.ex01_hello_world;

import java.time.Duration;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.sleep;

/**
 * 01 Hello world: a workflow that calls two tasks, one after the other.
 *
 * <p>A task is a {@code @WorkerTask} method on an ordinary class; a worker runs it wherever it is
 * hosted. A workflow is a {@code @Workflow} method on a class that takes the task class in its
 * constructor. In the workflow, {@code tasks.greet(name)} does not run the method here: it schedules
 * the task {@code greet} on the server, waits for it and returns its output. The workflow is
 * durable: if this process stops, the execution waits on the server and goes on when a worker is
 * back.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex01_hello_world
 * </pre>
 *
 * The execution's id is printed: open it in the Conductor UI, at {@code /execution/<id>}, to see the two tasks.
 */
public class HelloWorld {

    public static class Tasks {

        @WorkerTask("greet")
        public String greet(String name) {
            return "Hello, " + name;
        }

        @WorkerTask("shout")
        public String shout(String text) {
            return text.toUpperCase() + "!";
        }
    }

    private final Tasks tasks;

    /** Conductor supplies the tasks: each call schedules the task instead of running it here. */
    public HelloWorld(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "hello", version = 1)
    public String hello(String name) {
        String greeting = tasks.greet(name);
        sleep(Duration.ofSeconds(1));
        return tasks.shout(greeting);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            // host the workflow and its tasks in this process: workers poll the server for them
            conductor.workflows(HelloWorld.class).tasks(new Tasks()).start();

            WorkflowHandle<String> run = conductor.start(HelloWorld::hello, "World");
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
