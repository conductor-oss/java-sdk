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
package durability;

import java.util.List;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import durability.LedgerModel.LedgerInput;
import durability.LedgerModel.LedgerResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The bench workload, in memory: what a correct execution returns. */
class LedgerWorkflowTest {

    @Test
    void aCorrectExecutionSumsItsStepsAndKeepsItsSignals() {
        LedgerTasks tasks = mock(LedgerTasks.class);
        when(tasks.step(anyString(), anyInt()))
                .thenAnswer(i -> LedgerModel.expectedStep(i.getArgument(0), i.getArgument(1)));
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(LedgerWorkflow.class, LedgerChild.class).tasks(tasks);
            TestRun<LedgerResult> run = env.start(LedgerWorkflow::run, new LedgerInput("k-1", 2));
            run.signal(LedgerWorkflow::deposit, "dep-1").signal(LedgerWorkflow::deposit, "dep-2");
            assertEquals(1, (int) run.update(LedgerWorkflow::bump, "upd-1"));
            run.signal(LedgerWorkflow::close);

            LedgerResult result = run.result();
            assertEquals("k-1", result.id());
            assertEquals(LedgerModel.expectedTotal("k-1"), result.total());
            assertEquals(List.of("dep-1", "dep-2"), result.deposits());
            assertEquals(1, result.bumps());
            assertNotNull(result.token());
            assertTrue(run.ran("durability_ledger_child") || !run.ranTasks().isEmpty());
        }
    }
}
