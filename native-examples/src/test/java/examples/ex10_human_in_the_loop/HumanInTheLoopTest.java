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
package examples.ex10_human_in_the_loop;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex10_human_in_the_loop.HumanInTheLoop.Decision;
import examples.ex10_human_in_the_loop.HumanInTheLoop.Expense;
import examples.ex10_human_in_the_loop.HumanInTheLoop.Transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HumanInTheLoopTest {

    private static final Expense TRAVEL = new Expense("EXP-7", "ana", "mia", 840, "travel");

    private final DurableTestEnvironment env = DurableTestEnvironment.inMemory()
            .workflows(HumanInTheLoop.class)
            .tasks(new HumanInTheLoop.Tasks());

    @Test
    void approvedThenPaid() {
        env.human("manager_review").answer(new Decision(true, "ok"));
        env.external("bank_confirmation").complete(new Transfer("TX-1"));

        assertEquals("EXP-7: approved (ok), paid as TX-1", env.start(HumanInTheLoop::approve, TRAVEL).result());
    }

    @Test
    void escalatedWhenNobodyAnswers() {
        env.human("manager_review").timeout();

        assertEquals("EXP-7: no answer in three days, escalated", env.start(HumanInTheLoop::approve, TRAVEL).result());
    }

    @Test
    void aSmallExpenseNeedsNoReview() {
        var run = env.start(HumanInTheLoop::approve, new Expense("EXP-8", "ana", "mia", 40, "lunch"));

        assertEquals("EXP-8: approved without review", run.result());
        assertFalse(run.ran("manager_review"));
    }
}
