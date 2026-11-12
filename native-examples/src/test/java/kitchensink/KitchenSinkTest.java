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
package kitchensink;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.AgentCall;
import org.conductoross.conductor.sdk.McpCall;
import org.conductoross.conductor.sdk.testing.DurableReplay;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import kitchensink.KitchenSinkModel.Approval;
import kitchensink.KitchenSinkModel.Callback;
import kitchensink.KitchenSinkModel.CancelRequest;
import kitchensink.KitchenSinkModel.JobStatus;
import kitchensink.KitchenSinkModel.KitchenSinkInput;
import kitchensink.KitchenSinkModel.KitchenSinkResult;
import kitchensink.KitchenSinkModel.OddEven;
import kitchensink.KitchenSinkModel.Receipt;
import kitchensink.KitchenSinkModel.Stats;
import kitchensink.KitchenSinkModel.Summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * No Docker and no running server:
 *
 *   DurableTestEnvironment.inMemory()   runs the workflow code in process and answers server steps
 *                                       at once: timers are skipped, async lanes run one after the
 *                                       other, and HTTP, human, callback and LLM steps answer from
 *                                       the test. Tasks are a Mockito mock of the task class;
 *                                       tasks.task1(...) in the workflow lands on the mock.
 *
 *   DurableReplay                       re-runs the class against an execution recorded from a real
 *                                       server, exactly as a worker does, and fails on divergence, so
 *                                       a change that would break executions in flight fails in CI.
 *                                       The recording is written by Main when KITCHENSINK_EXPORT is set.
 */
class KitchenSinkTest {

    private final KitchenSinkTasks tasks = mock(KitchenSinkTasks.class);

    @Test
    void oddPathRunsTheForkAndBothSubFlows() {
        when(tasks.task1(5, "1")).thenReturn(new Stats(5, "1"));
        when(tasks.task5(5, "1")).thenReturn(new OddEven(5, "1"));   // also serves the dynamic task_2 call
        when(tasks.task6("1")).thenReturn("six");
        when(tasks.task30(any(), any())).thenReturn(new Summary(2));
        when(tasks.checkStatus(anyString())).thenReturn(new JobStatus(true, "finished"));
        when(tasks.charge(anyString(), any(Integer.class))).thenReturn(new Receipt("r1", 500));

        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(KitchenSinkWorkflow.class, SubFlow.class);
            env.tasks(tasks);
            env.http("get_es_1").respond("""
                    {"hits": [{"status": "COMPLETED", "workflowId": "w1"}]}
                    """);
            env.human("approval").answer(new Approval("finance-bot", true));
            env.external("callback").complete(new Callback("delivered"));
            env.llm("delivery_note").respond("All good.");
            stubAi(env);

            TestRun<KitchenSinkResult> run =
                    env.start(KitchenSinkWorkflow::kitchenSink, new KitchenSinkInput(5, "1", "task_5"));
            run.signal(KitchenSinkWorkflow::cancel, new CancelRequest("test"));
            KitchenSinkResult result = run.result();                   // timers are skipped, not waited for

            assertEquals("1", result.oddEven());
            assertEquals(List.of("COMPLETED"), result.statuses());
            assertEquals("finance-bot", result.approvedBy());
            assertEquals("delivered", result.callbackPayload());
            assertTrue(result.charged());
            assertTrue(result.cancelled());

            assertTrue(run.ran("fork"));                               // the graph is assertable too
            assertEquals(4, run.tasks("sub_flow_1").size());
            assertFalse(run.ran("fanout1"));
            assertEquals(List.of("LABEL[box-1]", "LABEL[box-2]", "LABEL[box-3]"), result.labels());
            assertEquals(2, result.toolCount());
            assertEquals("Sunny in Seattle", result.weather());
            assertEquals("reviewed", result.agentReply());
            assertTrue(run.ran("label_item") && run.ran("sleep_until") && run.ran("support_agent"));
            verify(tasks).task10();
            verify(tasks).task11();
        }
    }

    /** Answers for the MCP and agent steps, and the per-item label task. */
    private void stubAi(DurableTestEnvironment env) {
        when(tasks.labelItem(anyString())).thenAnswer(i -> "LABEL[" + i.getArgument(0) + "]");
        env.mcp("mcp_tools").respond(List.of(
                new McpCall.McpTool("get_weather", "weather", Map.of()),
                new McpCall.McpTool("add", "add", Map.of())));
        env.mcp("mcp_weather").respond(new McpCall.McpResult(List.of(Map.of("type", "text", "text", "Sunny in Seattle")), false));
        env.agent("support_agent").respond(new AgentCall.AgentResult("reviewed", "message", Map.of(), null, null));
    }

    @Test
    void refundsWhenTheStepAfterTheChargeFails() {
        when(tasks.task1(5, "1")).thenReturn(new Stats(5, "1"));
        when(tasks.task5(5, "1")).thenReturn(new OddEven(5, "1"));
        when(tasks.task6("1")).thenReturn("six");
        when(tasks.checkStatus(anyString())).thenReturn(new JobStatus(true, "finished"));
        when(tasks.charge(anyString(), any(Integer.class))).thenReturn(new Receipt("r1", 500));
        when(tasks.task30(any(), any()))
                .thenReturn(new Summary(2))                            // section 1
                .thenThrow(new IllegalStateException("ledger down")); // after the charge: retried, then TaskFailure

        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(KitchenSinkWorkflow.class, SubFlow.class);
            env.tasks(tasks);
            env.http("get_es_1").fail("connection refused");          // caught in the workflow, run continues
            env.human("approval").timeout();
            env.external("callback").timeout();
            env.llm("delivery_note").fail("no provider");             // caught too
            stubAi(env);

            TestRun<KitchenSinkResult> run =
                    env.start(KitchenSinkWorkflow::kitchenSink, new KitchenSinkInput(5, "1", "task_5"));
            KitchenSinkResult result = run.result();

            verify(tasks).refund(result.runId(), 500);
            assertTrue(result.charged());
            assertNull(result.approvedBy());
            assertEquals(List.of(), result.statuses());
            assertTrue(run.ran("get_es_1"));                          // the failed HTTP task is still in the graph
        }
    }

    @Test
    void currentCodeStillMatchesARecordedProductionRun() {
        // Recorded by Main with KITCHENSINK_EXPORT=src/test/resources/executions/kitchensink-odd.json
        DurableReplay.assertCompatible(
                KitchenSinkWorkflow.class,
                Path.of("src/test/resources/executions/kitchensink-odd.json"));
    }
}
