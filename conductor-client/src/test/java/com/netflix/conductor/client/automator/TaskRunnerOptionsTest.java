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
package com.netflix.conductor.client.automator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The per-runner options (update-v2, polling without domains), and the updates a runner sends: a
 * refused update once, a worker that throws once.
 */
@SuppressWarnings("unchecked") // generic test data read back from maps and mocks
class TaskRunnerOptionsTest {

    private TaskRunner runner;

    @AfterEach
    void tearDown() {
        if (runner != null) {
            runner.shutdown(1);
        }
    }

    private static Task task(String id) {
        Task task = new Task();
        task.setTaskId(id);
        task.setTaskDefName("code_type");
        task.setWorkflowInstanceId("wf");
        task.setStatus(Task.Status.IN_PROGRESS);
        return task;
    }

    private Worker worker() {
        Worker worker = mock(Worker.class);
        when(worker.getTaskDefName()).thenReturn("code_type");
        when(worker.getPollingInterval()).thenReturn(10);
        when(worker.getIdentity()).thenReturn("w");
        when(worker.execute(any())).thenAnswer(i -> {
            TaskResult result = new TaskResult(i.getArgument(0));
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        });
        return worker;
    }

    @SuppressWarnings("unchecked")
    private void run(Worker worker, TaskClient client) {
        // a domain is configured, and ignored
        runner = new TaskRunner(worker, client, 3, Map.of("code_type", "blue"), "test-%d", 2, 100, List.of(),
                (EventDispatcher<TaskRunnerEvent>) mock(EventDispatcher.class), true, true, false);
        Thread poller = new Thread(runner::pollAndExecute);
        poller.setDaemon(true);
        poller.start();
    }

    @Test
    void theNextTaskUpdateV2HandsBackRunsWithoutAPollAndNoDomainIsPolled() {
        Task first = task("t1");
        Task next = task("t2");
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        // after the first task, every poll is a long poll that finds nothing for 3 s
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(first))
                .thenAnswer(i -> {
                    Thread.sleep(3000);
                    return List.of();
                });
        when(client.updateTaskV2(any(), any())).thenReturn(next, (Task) null);
        Worker worker = worker();

        run(worker, client);

        // runs at once, not when the poll loop comes back from its long poll
        verify(worker, timeout(1500)).execute(next);
        verify(client, never()).batchPollTasksInDomain(any(), eq("blue"), any(), anyInt(), anyInt());
        verify(client, never()).updateTask(any(), any());
    }

    @Test
    void aWorkerThatThrowsIsReportedFailedOnce() {
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(task("t1")), List.of());
        Worker worker = worker();
        Mockito.doThrow(new IllegalStateException("boom")).when(worker).execute(any());

        run(worker, client);

        ArgumentCaptor<TaskResult> sent = ArgumentCaptor.forClass(TaskResult.class);
        verify(client, after(1500).times(1)).updateTaskV2(sent.capture(), any());
        Assertions.assertEquals(TaskResult.Status.FAILED, sent.getValue().getStatus());
        Assertions.assertTrue(sent.getValue().getReasonForIncompletion().contains("boom"));
    }

    @Test
    void aRefusedUpdateIsNotRetried() {
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(task("t1")), List.of());
        when(client.updateTaskV2(any(), any())).thenThrow(new ConductorClientException(409, "superseded"));

        run(worker(), client);

        verify(client, after(2500).times(1)).updateTaskV2(any(), any());
    }

    @Test
    void aWorkerPausedWhileATaskRunsTakesNoNextTaskFromItsUpdate() {
        Task first = task("t1");
        Task next = task("t2");
        AtomicBoolean paused = new AtomicBoolean();
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(first), List.of());
        // the queue has a backlog: update-v2 hands back the next task
        when(client.updateTaskV2(any(), any())).thenReturn(next, (Task) null);
        Worker worker = worker();
        when(worker.paused()).thenAnswer(i -> paused.get());
        when(worker.execute(first)).thenAnswer(i -> {
            paused.set(true); // paused while the first task runs
            TaskResult result = new TaskResult(first);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        });

        run(worker, client);

        // paused, it runs nothing more: the first task's result goes with the update that hands
        // nothing back
        verify(worker, after(1500).never()).execute(next);
        verify(client).updateTask(any(), any());
        verify(client, never()).updateTaskV2(any(), any());
    }

    @Test
    void aTaskFinishingDuringShutdownTakesNoNextTaskFromItsUpdate() throws Exception {
        Task first = task("t1");
        Task next = task("t2");
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(first), List.of());
        // the server leases the next task to whoever completes with update-v2
        when(client.updateTaskV2(any(), any())).thenReturn(next, (Task) null);
        Worker worker = worker();
        when(worker.execute(first)).thenAnswer(i -> {
            running.countDown();
            finish.await();
            TaskResult result = new TaskResult(first);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        });

        run(worker, client);
        running.await();
        Thread stopping = new Thread(() -> runner.shutdown(5)); // waits for the task in flight
        stopping.start();
        Thread.sleep(500);
        finish.countDown();
        stopping.join();

        // a task handed back now could not run (the executor refuses it) and would wait out its
        // lease: the result goes with the update that hands nothing back
        verify(client, never()).updateTaskV2(any(), any());
        verify(client).updateTask(any(), any());
        verify(worker, never()).execute(next);
    }

    @Test
    void aTaskHandedBackAfterShutdownBeganStillRuns() throws Exception {
        Task first = task("t1");
        Task next = task("t2");
        CountDownLatch updating = new CountDownLatch(1);
        CountDownLatch answer = new CountDownLatch(1);
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(first), List.of());
        // the first task's update is on its way when shutdown begins; the server answers it
        // with the next task, leased to this worker
        when(client.updateTaskV2(any(), any())).thenAnswer(i -> {
            updating.countDown();
            answer.await();
            return next;
        });
        Worker worker = worker();

        run(worker, client);
        updating.await();
        Thread stopping = new Thread(() -> runner.shutdown(5));
        stopping.start();
        Thread.sleep(500);
        answer.countDown();
        stopping.join();

        // the executor takes nothing new, so the task runs on the thread that received it; its
        // own result goes with the update that hands nothing back
        verify(worker).execute(next);
        verify(client).updateTask(any(), any());
        verify(client, Mockito.times(1)).updateTaskV2(any(), any());
    }

    @Test
    void theTasksOfAPollInFlightWhenShutdownBeginsStillRun() throws Exception {
        Task first = task("t1");
        CountDownLatch polling = new CountDownLatch(1);
        CountDownLatch answer = new CountDownLatch(1);
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        // the poll is on its way when shutdown begins; the server answers it with a task
        when(client.batchPollTasksInDomain(eq("code_type"), isNull(), anyString(), anyInt(), anyInt()))
                .thenAnswer(i -> {
                    polling.countDown();
                    answer.await();
                    return List.of(first);
                })
                .thenReturn(List.of());
        Worker worker = worker();

        run(worker, client);
        polling.await();
        Thread stopping = new Thread(() -> runner.shutdown(5));
        stopping.start();
        Thread.sleep(500);
        answer.countDown();
        stopping.join();

        verify(worker).execute(first);
        verify(client).updateTask(any(), any());
        verify(client, never()).updateTaskV2(any(), any());
    }
}
