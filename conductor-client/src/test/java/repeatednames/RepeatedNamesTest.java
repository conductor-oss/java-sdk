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
package repeatednames;

import java.util.List;

import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;

import static org.conductoross.conductor.sdk.Conductor.llm;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A name given in the code is numbered when the call is made again. The numbering counts calls as
 * they run, so a loop and the same calls written out get the same names.
 */
class RepeatedNamesTest {

    public static class Notes {

        @Workflow(name = "rn_loop")
        public String loop(@InputParam("x") String x) {
            String r = x;
            for (int i = 0; i < 3; i++) {
                r = llm("note").model("anthropic/claude-haiku-5-5").prompt(r).as(String.class);
            }
            return r;
        }

        @Workflow(name = "rn_unrolled")
        public String unrolled(@InputParam("x") String x) {
            String r = llm("note").model("anthropic/claude-haiku-5-5").prompt(x).as(String.class);
            r = llm("note").model("anthropic/claude-haiku-5-5").prompt(r).as(String.class);
            return llm("note").model("anthropic/claude-haiku-5-5").prompt(r).as(String.class);
        }
    }

    @Test
    void aNamedCallInALoopIsNumberedByItsTurn() {
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(Notes.class).llm("note").respond("ok");
            TestRun<String> run = env.start(Notes::loop, "x");
            assertEquals("ok", run.result());
            assertEquals(List.of("note", "note_2", "note_3"), run.ranTasks());
        }
    }

    @Test
    void theSameCallsWrittenOutGetTheSameNamesAsTheLoop() {
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(Notes.class).llm("note").respond("ok");
            TestRun<String> run = env.start(Notes::unrolled, "x");
            assertEquals("ok", run.result());
            assertEquals(List.of("note", "note_2", "note_3"), run.ranTasks());
        }
    }

    @Test
    void aStubForANumberedNameAnswersOnlyThatTurn() {
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(Notes.class).llm("note").respond("every turn").llm("note_3").respond("the last turn");
            assertEquals("the last turn", env.start(Notes::loop, "x").result());
        }
    }
}
