/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.common.run.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class TypedTaskTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        objectMapper = new ObjectMapper();
    }

    private Task loadTask(String filename) throws IOException {
        String resourcePath = "tasks/" + filename;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, Task.class);
        }
    }

    @Nested
    class WaitTaskTests {

        @Test
        void testWaitDurationBased() throws IOException {
            Task task = loadTask("wait_duration.json");

            WaitTask wait = new WaitTask(task);

            assertTrue(wait.isDurationBased());
            assertFalse(wait.isUntilBased());
            assertFalse(wait.isSignalBased());
            assertEquals(WaitTask.WaitType.DURATION, wait.getWaitType());

            assertEquals("30s", wait.getDurationString());
            assertEquals(Duration.ofSeconds(30), wait.getDuration());

            assertNull(wait.getUntilString());
            assertNull(wait.getUntil());
        }

        @Test
        void testWaitUntilBased() throws IOException {
            Task task = loadTask("wait_until.json");

            WaitTask wait = new WaitTask(task);

            assertFalse(wait.isDurationBased());
            assertTrue(wait.isUntilBased());
            assertFalse(wait.isSignalBased());
            assertEquals(WaitTask.WaitType.UNTIL, wait.getWaitType());

            assertEquals("2025-12-31 23:59 UTC", wait.getUntilString());

            ZonedDateTime until = wait.getUntil();
            assertNotNull(until);
            assertEquals(2025, until.getYear());
            assertEquals(12, until.getMonthValue());
            assertEquals(31, until.getDayOfMonth());

            assertNull(wait.getDurationString());
        }

        @Test
        void testWaitUntilWithGmtOffset() throws IOException {
            Task task = loadTask("wait_until_gmt_offset.json");

            WaitTask wait = new WaitTask(task);

            assertTrue(wait.isUntilBased());
            assertEquals("2026-01-31 00:00 GMT-03:00", wait.getUntilString());

            ZonedDateTime until = wait.getUntil();
            assertNotNull(until);
            assertEquals(2026, until.getYear());
            assertEquals(1, until.getMonthValue());
            assertEquals(31, until.getDayOfMonth());
            assertEquals(0, until.getHour());
            assertEquals(-3, until.getOffset().getTotalSeconds() / 3600);
        }

        @Test
        void testWaitSignalBased() throws IOException {
            Task task = loadTask("wait_signal.json");

            WaitTask wait = new WaitTask(task);

            assertFalse(wait.isDurationBased());
            assertFalse(wait.isUntilBased());
            assertTrue(wait.isSignalBased());
            assertEquals(WaitTask.WaitType.SIGNAL, wait.getWaitType());

            assertNull(wait.getDurationString());
            assertNull(wait.getUntilString());
        }

        @Test
        void testWaitTaskViaTaskAs() throws IOException {
            Task task = loadTask("wait_duration.json");

            WaitTask wait = task.as(WaitTask.class);

            assertTrue(wait.isDurationBased());
            assertEquals("30s", wait.getDurationString());
        }

        @Test
        void testWaitTaskThrowsForWrongType() throws IOException {
            Task task = loadTask("http.json");

            assertThrows(IllegalArgumentException.class, () -> new WaitTask(task));
            assertThrows(IllegalArgumentException.class, () -> task.as(WaitTask.class));
        }

        @Test
        void testIsWaitTask() throws IOException {
            Task waitTask = loadTask("wait_duration.json");
            Task httpTask = loadTask("http.json");

            assertTrue(WaitTask.isWaitTask(waitTask));
            assertFalse(WaitTask.isWaitTask(httpTask));
            assertFalse(WaitTask.isWaitTask(null));
        }
    }

    @Nested
    class HttpTaskTests {

        @Test
        void testHttpTaskNestedFormat() throws IOException {
            Task task = loadTask("http.json");

            HttpTask http = new HttpTask(task);

            assertTrue(http.isNestedFormat());
            assertEquals("https://api.example.com/users", http.getUri());
            assertEquals("POST", http.getMethod());

            Map<String, Object> headers = http.getRequestHeaders();
            assertEquals("application/json", headers.get("Content-Type"));
            assertEquals("Bearer token123", headers.get("Authorization"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) http.getRequestBody();
            assertNotNull(body);
            assertEquals("John Doe", body.get("name"));
            assertEquals("john@example.com", body.get("email"));

            assertEquals(5000, http.getConnectionTimeout());
            assertEquals(10000, http.getReadTimeout());

            // Test asyncComplete and createdBy
            assertEquals(false, http.isAsyncComplete());
            assertEquals("john.doe@example.com", http.getCreatedBy());
        }

        @Test
        void testHttpTaskFlatFormat() throws IOException {
            Task task = loadTask("http_flat.json");

            HttpTask http = new HttpTask(task);

            assertFalse(http.isNestedFormat());
            assertEquals("https://orkes-api-tester.orkesconductor.com/api", http.getUri());
            assertEquals("GET", http.getMethod());

            // Flat format specific fields
            assertEquals("application/json", http.getContentType());
            assertEquals("application/json", http.getAccept());
            assertEquals(true, http.isEncode());
            assertEquals(false, http.isAsyncComplete());

            // Headers should be built from contentType and accept
            Map<String, Object> headers = http.getRequestHeaders();
            assertEquals("application/json", headers.get("Content-Type"));
            assertEquals("application/json", headers.get("Accept"));

            // Test createdBy
            assertEquals("miguel.prieto@orkes.io", http.getCreatedBy());
        }

        @Test
        void testHttpTaskResponse() throws IOException {
            Task task = loadTask("http.json");

            HttpTask http = new HttpTask(task);

            assertEquals(201, http.getStatusCode());
            assertTrue(http.isSuccessful());
            assertEquals("Created", http.getReasonPhrase());

            Map<String, Object> responseHeaders = http.getResponseHeaders();
            assertEquals("application/json", responseHeaders.get("Content-Type"));

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) http.getResponseBody();
            assertNotNull(responseBody);
            assertEquals("user-123", responseBody.get("id"));
        }

        @Test
        void testHttpTaskFlatFormatResponse() throws IOException {
            Task task = loadTask("http_flat.json");

            HttpTask http = new HttpTask(task);

            assertEquals(200, http.getStatusCode());
            assertTrue(http.isSuccessful());
            assertEquals("OK", http.getReasonPhrase());
        }

        @Test
        void testHttpTaskViaTaskAs() throws IOException {
            Task task = loadTask("http.json");

            HttpTask http = task.as(HttpTask.class);

            assertEquals("https://api.example.com/users", http.getUri());
            assertTrue(http.isSuccessful());
        }

        @Test
        void testIsHttpTask() throws IOException {
            Task httpTask = loadTask("http.json");
            Task waitTask = loadTask("wait_duration.json");

            assertTrue(HttpTask.isHttpTask(httpTask));
            assertFalse(HttpTask.isHttpTask(waitTask));
        }
    }

    @Nested
    class SwitchTaskTests {

        @Test
        void testSwitchTask() throws IOException {
            Task task = loadTask("switch.json");

            SwitchTask switchTask = new SwitchTask(task);

            assertEquals("javascript", switchTask.getEvaluatorType());
            assertTrue(switchTask.isJavaScriptEvaluator());
            assertFalse(switchTask.isValueParamEvaluator());

            assertEquals("$.type == 'premium' ? 'premium' : 'standard'", switchTask.getExpression());
            assertEquals("premium", switchTask.getSelectedCase());
        }

        @Test
        void testSwitchTaskViaTaskAs() throws IOException {
            Task task = loadTask("switch.json");

            SwitchTask switchTask = task.as(SwitchTask.class);

            assertEquals("premium", switchTask.getSelectedCase());
        }

        @Test
        void testIsSwitchTask() throws IOException {
            Task switchTask = loadTask("switch.json");
            Task httpTask = loadTask("http.json");

            assertTrue(SwitchTask.isSwitchTask(switchTask));
            assertFalse(SwitchTask.isSwitchTask(httpTask));
        }
    }

    @Nested
    class SubWorkflowTaskTests {

        @Test
        void testSubWorkflowTask() throws IOException {
            Task task = loadTask("sub_workflow.json");

            SubWorkflowTask subWf = new SubWorkflowTask(task);

            assertEquals("order_processing_workflow", subWf.getSubWorkflowName());
            assertEquals(2, subWf.getSubWorkflowVersion());
            assertEquals("sub-workflow-123", subWf.getSubWorkflowId());
        }

        @Test
        void testSubWorkflowTaskViaTaskAs() throws IOException {
            Task task = loadTask("sub_workflow.json");

            SubWorkflowTask subWf = task.as(SubWorkflowTask.class);

            assertEquals("sub-workflow-123", subWf.getSubWorkflowId());
        }

        @Test
        void testIsSubWorkflowTask() throws IOException {
            Task subWorkflowTask = loadTask("sub_workflow.json");
            Task httpTask = loadTask("http.json");

            assertTrue(SubWorkflowTask.isSubWorkflowTask(subWorkflowTask));
            assertFalse(SubWorkflowTask.isSubWorkflowTask(httpTask));
        }
    }

    @Nested
    class EventTaskTests {

        @Test
        void testEventTask() throws IOException {
            Task task = loadTask("event.json");

            EventTask event = new EventTask(task);

            assertEquals("kafka:notifications-topic", event.getSink());
            assertEquals("kafka", event.getSinkType());
            assertEquals("notifications-topic", event.getSinkTarget());

            Map<String, Object> payload = event.getEventPayload();
            assertEquals("user-123", payload.get("userId"));
            assertEquals("Order completed", payload.get("message"));
            assertEquals("high", payload.get("priority"));
            assertFalse(payload.containsKey("sink")); // sink should be excluded

            assertTrue(event.isEventPublished());
            Map<String, Object> produced = event.getEventProduced();
            assertEquals("event-abc", produced.get("id"));
        }

        @Test
        void testEventTaskViaTaskAs() throws IOException {
            Task task = loadTask("event.json");

            EventTask event = task.as(EventTask.class);

            assertEquals("kafka", event.getSinkType());
        }

        @Test
        void testIsEventTask() throws IOException {
            Task eventTask = loadTask("event.json");
            Task httpTask = loadTask("http.json");

            assertTrue(EventTask.isEventTask(eventTask));
            assertFalse(EventTask.isEventTask(httpTask));
        }
    }

    @Nested
    class TerminateTaskTests {

        @Test
        void testTerminateTask() throws IOException {
            Task task = loadTask("terminate.json");

            TerminateTask terminate = new TerminateTask(task);

            assertEquals(Workflow.WorkflowStatus.FAILED, terminate.getTerminationStatus());
            assertEquals("FAILED", terminate.getTerminationStatusString());
            assertTrue(terminate.isTerminatingAsFailed());
            assertFalse(terminate.isTerminatingAsCompleted());

            assertEquals("Validation failed: missing required field", terminate.getTerminationReason());

            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) terminate.getWorkflowOutput();
            assertNotNull(output);
            assertEquals("validation_error", output.get("error"));
        }

        @Test
        void testTerminateTaskViaTaskAs() throws IOException {
            Task task = loadTask("terminate.json");

            TerminateTask terminate = task.as(TerminateTask.class);

            assertTrue(terminate.isTerminatingAsFailed());
        }

        @Test
        void testIsTerminateTask() throws IOException {
            Task terminateTask = loadTask("terminate.json");
            Task httpTask = loadTask("http.json");

            assertTrue(TerminateTask.isTerminateTask(terminateTask));
            assertFalse(TerminateTask.isTerminateTask(httpTask));
        }
    }

    @Nested
    class DoWhileTaskTests {

        @Test
        void testDoWhileTask() throws IOException {
            Task task = loadTask("do_while.json");

            DoWhileTask doWhile = new DoWhileTask(task);

            assertEquals(3, doWhile.getCurrentIteration());
            assertFalse(doWhile.isFirstIteration());

            String condition = doWhile.getLoopCondition();
            assertNotNull(condition);
            assertTrue(condition.contains("$.iteration < 5"));

            assertEquals(3, doWhile.getIterationCount());
        }

        @Test
        void testDoWhileTaskViaTaskAs() throws IOException {
            Task task = loadTask("do_while.json");

            DoWhileTask doWhile = task.as(DoWhileTask.class);

            assertEquals(3, doWhile.getCurrentIteration());
        }

        @Test
        void testIsDoWhileTask() throws IOException {
            Task doWhileTask = loadTask("do_while.json");
            Task httpTask = loadTask("http.json");

            assertTrue(DoWhileTask.isDoWhileTask(doWhileTask));
            assertFalse(DoWhileTask.isDoWhileTask(httpTask));
        }
    }

    @Nested
    class ForkJoinTaskTests {

        @Test
        void testForkJoinTask() throws IOException {
            Task task = loadTask("fork_join.json");

            ForkJoinTask fork = new ForkJoinTask(task);

            assertFalse(fork.isDynamic());
            assertEquals(3, fork.getForkBranchCount());

            var joinOn = fork.getJoinOn();
            assertEquals(3, joinOn.size());
            assertTrue(joinOn.contains("task_a"));
            assertTrue(joinOn.contains("task_b"));
            assertTrue(joinOn.contains("task_c"));

            var forkTasks = fork.getForkTasks();
            assertEquals(3, forkTasks.size());
        }

        @Test
        void testForkJoinDynamicTask() throws IOException {
            Task task = loadTask("fork_join_dynamic.json");

            ForkJoinTask fork = new ForkJoinTask(task);

            assertTrue(fork.isDynamic());

            var joinOn = fork.getJoinOn();
            assertEquals(2, joinOn.size());
            assertTrue(joinOn.contains("dynamic_task_1"));
            assertTrue(joinOn.contains("dynamic_task_2"));
        }

        @Test
        void testForkJoinTaskViaTaskAs() throws IOException {
            Task task = loadTask("fork_join.json");

            ForkJoinTask fork = task.as(ForkJoinTask.class);

            assertFalse(fork.isDynamic());
            assertEquals(3, fork.getForkBranchCount());
        }

        @Test
        void testIsForkJoinTask() throws IOException {
            Task forkTask = loadTask("fork_join.json");
            Task dynamicForkTask = loadTask("fork_join_dynamic.json");
            Task httpTask = loadTask("http.json");

            assertTrue(ForkJoinTask.isForkJoinTask(forkTask));
            assertTrue(ForkJoinTask.isForkJoinTask(dynamicForkTask));
            assertFalse(ForkJoinTask.isForkJoinTask(httpTask));
        }
    }

    @Nested
    class HumanTaskTests {

        @Test
        void testHumanTask() throws IOException {
            Task task = loadTask("human.json");

            HumanTask human = new HumanTask(task);

            assertEquals("manager@example.com", human.getAssignee());
            assertTrue(human.isAssigned());
            assertEquals("Approve Purchase Order", human.getDisplayName());
            assertEquals("approval_form_v1", human.getFormTemplate());

            assertTrue(human.hasFormData());
            Map<String, Object> formData = human.getFormData();
            assertEquals(true, formData.get("approved"));
            assertEquals("Approved for Q1 budget", formData.get("comments"));

            assertEquals("john.manager@example.com", human.getCompletedBy());
        }

        @Test
        void testHumanTaskViaTaskAs() throws IOException {
            Task task = loadTask("human.json");

            HumanTask human = task.as(HumanTask.class);

            assertEquals("manager@example.com", human.getAssignee());
        }

        @Test
        void testIsHumanTask() throws IOException {
            Task humanTask = loadTask("human.json");
            Task httpTask = loadTask("http.json");

            assertTrue(HumanTask.isHumanTask(humanTask));
            assertFalse(HumanTask.isHumanTask(httpTask));
        }
    }

    @Nested
    class TypedTaskBaseTests {

        @Test
        void testCommonProperties() throws IOException {
            Task task = loadTask("wait_duration.json");

            WaitTask wait = new WaitTask(task);

            assertEquals("wait-task-001", wait.getTaskId());
            assertEquals("wait_for_duration", wait.getReferenceTaskName());
            assertEquals("workflow-001", wait.getWorkflowInstanceId());
            assertEquals("WAIT", wait.getTaskType());
            assertEquals(Task.Status.IN_PROGRESS, wait.getStatus());

            // getTask returns the underlying task
            assertSame(task, wait.getTask());
        }

        @Test
        void testGetCreatedBy() throws IOException {
            Task httpTask = loadTask("http.json");
            Task waitTask = loadTask("wait_duration.json");

            HttpTask http = new HttpTask(httpTask);
            WaitTask wait = new WaitTask(waitTask);

            // HTTP task has _createdBy
            assertEquals("john.doe@example.com", http.getCreatedBy());

            // Wait task does not have _createdBy
            assertNull(wait.getCreatedBy());
        }

        @Test
        void testNullTaskThrows() {
            assertThrows(IllegalArgumentException.class, () -> new WaitTask(null));
            assertThrows(IllegalArgumentException.class, () -> new HttpTask(null));
            assertThrows(IllegalArgumentException.class, () -> new SwitchTask(null));
        }
    }
}
