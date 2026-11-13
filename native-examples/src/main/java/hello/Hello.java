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
package hello;

import java.time.Duration;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;


/** Hello world: a workflow that calls two tasks, one after the other. */
public class Hello {

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
    public Hello(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "hello", version = 1)
    public String hello(String name) {
        String greeting = tasks.greet(name);
        return tasks.shout(greeting);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(Hello.class).tasks(new Tasks()).start();

        WorkflowHandle<String> run = conductor.start(Hello::hello, "World");
        System.out.println("execution: " + run.url());
        System.out.println("result:    " + run.result(Duration.ofSeconds(30)));

        conductor.close();

        client.shutdown();
    }
}
