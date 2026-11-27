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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskRunnerAdaptiveTest {

    private static final String TASK_TYPE = "adaptive_task";

    private final List<TaskRunner> runners = new ArrayList<>();
    private final List<Thread> pollThreads = new ArrayList<>();

    @AfterEach
    void tearDown() {
        runners.forEach(runner -> runner.shutdown(1));
        pollThreads.forEach(Thread::interrupt);
    }

    private static AdaptiveConcurrencyController controller() {
        var config = new AdaptiveConcurrencyConfig.Builder()
                .withDemandOnlyEnabled(true)
                .withInitialProcessConcurrency(4)
                .build();
        var noMetrics = new AdaptiveConcurrencyControllerTest.FakeResourceSampler();
        noMetrics.cpuSupported = false;
        noMetrics.heapSupported = false;
        return new AdaptiveConcurrencyController(config, () -> noMetrics, System::nanoTime, false);
    }

    private static Worker worker(AtomicInteger executions, Set<String> threadNames, CountDownLatch done) {
        Worker worker = mock(Worker.class);
        when(worker.getTaskDefName()).thenReturn(TASK_TYPE);
        when(worker.getPollingInterval()).thenReturn(10);
        when(worker.getIdentity()).thenReturn("adaptive-worker-1");
        when(worker.execute(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            executions.incrementAndGet();
            threadNames.add(Thread.currentThread().getName());
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            done.countDown();
            return result;
        });
        return worker;
    }

    private static Task task() {
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTaskDefName(TASK_TYPE);
        task.setTaskType(TASK_TYPE);
        task.setStatus(Task.Status.IN_PROGRESS);
        return task;
    }

    private static TaskClient taskClient(ConcurrentLinkedQueue<Task> backlog) {
        TaskClient taskClient = mock(TaskClient.class);
        when(taskClient.batchPollTasksInDomain(anyString(), any(), anyString(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    int count = invocation.getArgument(3);
                    List<Task> tasks = new ArrayList<>();
                    Task next;
                    while (tasks.size() < count && (next = backlog.poll()) != null) {
                        tasks.add(next);
                    }
                    return tasks;
                });
        when(taskClient.getQueueSizesForTasks(anyList()))
                .thenAnswer(invocation -> Map.of(TASK_TYPE, backlog.size()));
        return taskClient;
    }

    private TaskRunner runner(Worker worker, TaskClient taskClient, AdaptiveConcurrencyController controller,
                              boolean useVirtualThreads, boolean updateV2) {
        @SuppressWarnings("unchecked")
        EventDispatcher<TaskRunnerEvent> eventDispatcher =
                (EventDispatcher<TaskRunnerEvent>) mock(EventDispatcher.class);
        TaskRunner runner = new TaskRunner(worker, taskClient, 3, Map.of(), "adaptive-worker-%d", 1, 100, List.of(),
                eventDispatcher, useVirtualThreads, updateV2, controller, 8);
        runners.add(runner);
        return runner;
    }

    private void start(TaskRunner runner) {
        Thread thread = new Thread(runner::pollAndExecute);
        thread.setDaemon(true);
        thread.start();
        pollThreads.add(thread);
    }

    private static void awaitHeldSlots(AdaptiveConcurrencyController controller, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (controller.getHeldSlots() != expected && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, controller.getHeldSlots());
    }

    @Test
    void polledTasksExecuteAndReleaseTheirSlots() throws Exception {
        var backlog = new ConcurrentLinkedQueue<Task>();
        for (int i = 0; i < 20; i++) {
            backlog.add(task());
        }
        var executions = new AtomicInteger();
        var done = new CountDownLatch(20);
        TaskClient taskClient = taskClient(backlog);
        AdaptiveConcurrencyController controller = controller();
        TaskRunner runner = runner(worker(executions, ConcurrentHashMap.newKeySet(), done), taskClient, controller,
                false, false);
        controller.activate();
        controller.sampleDepths();

        start(runner);

        assertTrue(done.await(10, TimeUnit.SECONDS), "all tasks executed");
        awaitHeldSlots(controller, 0);
        assertEquals(20, executions.get());
        verify(taskClient, never()).updateTaskV2(any(), anyString());
    }

    @Test
    void updateAndFetchReplacementReusesTheSlot() throws Exception {
        var executions = new AtomicInteger();
        var done = new CountDownLatch(5);
        var backlog = new ConcurrentLinkedQueue<Task>();
        backlog.add(task());
        var replacements = new ConcurrentLinkedQueue<Task>();
        for (int i = 0; i < 4; i++) {
            replacements.add(task());
        }
        TaskClient taskClient = taskClient(backlog);
        when(taskClient.updateTaskV2(any(TaskResult.class), anyString())).thenAnswer(invocation -> replacements.poll());
        AdaptiveConcurrencyController controller = controller();
        TaskRunner runner = runner(worker(executions, ConcurrentHashMap.newKeySet(), done), taskClient, controller,
                false, true);
        controller.activate();

        start(runner);

        assertTrue(done.await(10, TimeUnit.SECONDS), "polled task and four replacements executed");
        awaitHeldSlots(controller, 0);
        assertEquals(5, executions.get());
    }

    @Test
    void virtualThreadsRunEachTaskOnANumberedVirtualThread() throws Exception {
        var backlog = new ConcurrentLinkedQueue<Task>();
        for (int i = 0; i < 6; i++) {
            backlog.add(task());
        }
        var threadNames = ConcurrentHashMap.<String>newKeySet();
        var done = new CountDownLatch(6);
        AdaptiveConcurrencyController controller = controller();
        TaskRunner runner = runner(worker(new AtomicInteger(), threadNames, done), taskClient(backlog), controller,
                true, false);
        controller.activate();
        controller.sampleDepths();

        start(runner);

        assertTrue(done.await(10, TimeUnit.SECONDS));
        awaitHeldSlots(controller, 0);
        assertTrue(threadNames.stream().allMatch(name -> name.matches("adaptive-worker-\\d+")), threadNames.toString());
    }

    @Test
    void pollersOfOneQueueShareStateAndPool() throws Exception {
        var backlog = new ConcurrentLinkedQueue<Task>();
        var executions = new AtomicInteger();
        Worker worker = worker(executions, ConcurrentHashMap.newKeySet(), new CountDownLatch(0));
        TaskClient taskClient = taskClient(backlog);
        AdaptiveConcurrencyController controller = controller();
        TaskRunner first = runner(worker, taskClient, controller, false, false);
        TaskRunner second = runner(worker, taskClient, controller, false, false);
        controller.activate();

        WorkerQueue queue = extractQueue(first);
        assertSame(queue, extractQueue(second));
        assertEquals(2, queue.registrations);

        first.shutdown(1);
        runners.remove(first);
        assertTrue(!queue.executionPool().isShutdown(), "the remaining poller keeps the shared pool");
        second.shutdown(1);
        runners.remove(second);
        assertTrue(queue.executionPool().isShutdown());
    }

    @Test
    void pollTimeoutAboveServerLimitIsRejected() {
        var builder = new TaskRunnerConfigurer.Builder(mock(TaskClient.class), List.of())
                .withTaskPollTimeout(6000)
                .withAdaptiveConcurrency(controller());
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    private static WorkerQueue extractQueue(TaskRunner runner) throws Exception {
        var field = TaskRunner.class.getDeclaredField("workerQueue");
        field.setAccessible(true);
        return (WorkerQueue) field.get(runner);
    }
}
