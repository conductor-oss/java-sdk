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

import java.util.List;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing a workflow without a server: the in-memory environment runs the workflow code and the
 * tasks in this process, in order, and records which tasks ran. Every example has a test like this.
 */
class HelloWorldTest {

    /** The sleep is a step too; the in-memory environment does not wait it out. */
    @Test
    void greetsSleepsThenShouts() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory()
                .workflows(HelloWorld.class)
                .tasks(new HelloWorld.Tasks());

        TestRun<String> run = env.start(HelloWorld::hello, "World");

        assertEquals("HELLO, WORLD!", run.result());
        assertEquals(List.of("greet", "sleep", "shout"), run.ranTasks());
    }
}
