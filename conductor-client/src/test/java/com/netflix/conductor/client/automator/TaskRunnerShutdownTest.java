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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import io.orkes.conductor.client.http.FatalAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What a runner does with work the server has leased to it when the runner shuts down, and with an
 * update the server refuses.
 */
class TaskRunnerShutdownTest {

    private TaskRunner runner;

    @AfterEach
    void tearDown() {
        System.clearProperty("taskUpdateV2");
        if (runner != null) {
            runner.shutdown(1);
        }
    }

    private static Task task(String id) {
        Task task = new Task();
        task.setTaskId(id);
        task.setTaskDefName("test_task");
        task.setWorkflowInstanceId("wf");
        task.setStatus(Task.Status.IN_PROGRESS);
        return task;
    }

    private static Worker worker() {
        Worker worker = mock(Worker.class);
        when(worker.getTaskDefName()).thenReturn("test_task");
        when(worker.getPollingInterval()).thenReturn(10);
        when(worker.getIdentity()).thenReturn("w");
        when(worker.execute(any())).thenAnswer(i -> {
            TaskResult result = new TaskResult(i.getArgument(0));
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        });
        return worker;
    }

    private static TaskClient client() {
        TaskClient client = mock(TaskClient.class);
        when(client.evaluateAndUploadLargePayload(any(), any())).thenReturn(Optional.empty());
        return client;
    }

    private void run(Worker worker, TaskClient client) {
        run(worker, client, () -> {});
    }

    @SuppressWarnings("unchecked")
    private void run(Worker worker, TaskClient client, Runnable exitAction) {
        runner = new TaskRunner(worker, client, 3, Map.of(), "test-%d", 2, 100, List.of(),
                (EventDispatcher<TaskRunnerEvent>) mock(EventDispatcher.class), false);
        runner.setExitAction(exitAction); // never System.exit in a test
        Thread poller = new Thread(runner::pollAndExecute);
        poller.setDaemon(true);
        poller.start();
    }

    private void shutDownWhile(CountDownLatch inFlight, CountDownLatch answer) throws InterruptedException {
        inFlight.await();
        Thread stopping = new Thread(() -> runner.shutdown(5));
        stopping.start();
        Thread.sleep(500);
        answer.countDown();
        stopping.join();
    }

    @Test
    void aRefusedUpdateIsSentOnce() {
        TaskClient client = client();
        when(client.batchPollTasksInDomain(eq("test_task"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(task("t1")), List.of());
        updateFailsOnce(client, new ConductorClientException(404, "no such task"));

        run(worker(), client);

        // the same request is refused again: a retry, 2 s later, cannot help
        verify(client, after(2500).times(1)).updateTask(any(), any());
    }

    @Test
    void aThrottledUpdateIsRetried() {
        TaskClient client = client();
        when(client.batchPollTasksInDomain(eq("test_task"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(task("t1")), List.of());
        updateFailsOnce(client, new ConductorClientException(429, "slow down"));

        run(worker(), client);

        verify(client, timeout(3000).times(2)).updateTask(any(), any());
    }

    private static void updateFailsOnce(TaskClient client, RuntimeException failure) {
        doThrow(failure).doNothing().when(client).updateTask(any(), any());
    }

    @Test
    void theTasksOfAPollInFlightWhenShutdownBeginsStillRun() throws Exception {
        Task first = task("t1");
        CountDownLatch polling = new CountDownLatch(1);
        CountDownLatch answer = new CountDownLatch(1);
        TaskClient client = client();
        // the poll is on its way when shutdown begins; the server answers it with a task, leased
        // to this worker
        when(client.batchPollTasksInDomain(eq("test_task"), isNull(), anyString(), anyInt(), anyInt()))
                .thenAnswer(i -> {
                    polling.countDown();
                    answer.await();
                    return List.of(first);
                })
                .thenReturn(List.of());
        Worker worker = worker();

        run(worker, client);
        shutDownWhile(polling, answer);

        verify(worker).execute(first);
        verify(client).updateTask(any(), any());
    }

    @Test
    void shutdownDoesNotWaitForAPollLoopThatIsTerminatingTheJvm() throws Exception {
        CountDownLatch exiting = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        TaskClient client = client();
        when(client.batchPollTasksInDomain(eq("test_task"), isNull(), anyString(), anyInt(), anyInt()))
                .thenThrow(new FatalAuthenticationException("revoked"));

        // System.exit runs the shutdown hooks, one of which shuts this runner down, and returns
        // only when they are done
        run(worker(), client, () -> {
            exiting.countDown();
            try {
                exited.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        exiting.await();
        long start = System.nanoTime();
        runner.shutdown(5);
        long millis = (System.nanoTime() - start) / 1_000_000;
        exited.countDown();

        assertTrue(millis < 2000, "shutdown waited " + millis + " ms for a poll loop in System.exit");
    }

    @Test
    void withUpdateV2ATaskFinishingDuringShutdownTakesNoNextTask() throws Exception {
        System.setProperty("taskUpdateV2", "true");
        Task first = task("t1");
        Task next = task("t2");
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        TaskClient client = client();
        when(client.batchPollTasksInDomain(eq("test_task"), isNull(), anyString(), anyInt(), anyInt()))
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
        shutDownWhile(running, finish);

        // the poll loop has ended and takes no task handed back now: the result goes with the
        // update that hands nothing back
        verify(client, never()).updateTaskV2(any(), any());
        verify(client).updateTask(any(), any());
        verify(worker, never()).execute(next);
    }

    @Test
    void withUpdateV2ATaskHandedBackAsShutdownBeginsStillRuns() throws Exception {
        System.setProperty("taskUpdateV2", "true");
        Task first = task("t1");
        Task next = task("t2");
        CountDownLatch updating = new CountDownLatch(1);
        CountDownLatch answer = new CountDownLatch(1);
        TaskClient client = client();
        when(client.batchPollTasksInDomain(eq("test_task"), isNull(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(first), List.of());
        // the first task's update is on its way when shutdown begins; the server answers it with
        // the next task, leased to this worker
        when(client.updateTaskV2(any(), any())).thenAnswer(i -> {
            updating.countDown();
            answer.await();
            return next;
        });
        Worker worker = worker();

        run(worker, client);
        shutDownWhile(updating, answer);

        // it runs before shutdown returns; its own result goes with the update that hands nothing
        // back
        verify(worker).execute(next);
        verify(client, times(1)).updateTaskV2(any(), any());
        verify(client).updateTask(any(), any());
    }
}
