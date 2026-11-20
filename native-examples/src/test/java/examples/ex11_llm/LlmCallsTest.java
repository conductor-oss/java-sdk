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
package examples.ex11_llm;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex11_llm.LlmCalls.Handled;
import examples.ex11_llm.LlmCalls.Message;
import examples.ex11_llm.LlmCalls.Triage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The model's answers are given by the test: no model and no server. */
class LlmCallsTest {

    @Test
    void readsTheModelsJsonIntoARecord() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(LlmCalls.class);
        env.llm("classify").respond("{\"team\": \"shipping\", \"reason\": \"a late parcel\", \"urgent\": true}");
        env.llm("draft_reply").respond("Sorry about the delay!");

        Handled handled = env.start(LlmCalls::handle, new Message("lee", "Where is my parcel?")).result();

        assertEquals(new Handled(new Triage("shipping", "a late parcel", true), "Sorry about the delay!"), handled);
    }
}
