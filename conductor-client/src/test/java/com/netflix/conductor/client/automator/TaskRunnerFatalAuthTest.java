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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;

import io.orkes.conductor.client.http.FatalAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskRunnerFatalAuthTest {

    private TaskRunner runner;

    @AfterEach
    void tearDown() {
        if (runner != null) {
            runner.shutdown(1);
        }
    }

    @Test
    void exitActionInvokedOnFatalAuthenticationException() throws Exception {
        Worker worker = mock(Worker.class);
        when(worker.getTaskDefName()).thenReturn("test_task");
        when(worker.getPollingInterval()).thenReturn(100);
        when(worker.getIdentity()).thenReturn("test-worker-1");

        TaskClient taskClient = mock(TaskClient.class);
        when(taskClient.batchPollTasksInDomain(anyString(), any(), anyString(), anyInt(), anyInt()))
                .thenThrow(new FatalAuthenticationException("credentials revoked"));

        @SuppressWarnings("unchecked")
        EventDispatcher<TaskRunnerEvent> eventDispatcher =
                (EventDispatcher<TaskRunnerEvent>) mock(EventDispatcher.class);

        runner = new TaskRunner(
                worker,
                taskClient,
                3,
                Map.of(),
                "test-worker-",
                1,
                100,
                List.of(),
                eventDispatcher,
                false,
                null);

        CountDownLatch exitLatch = new CountDownLatch(1);
        runner.setExitAction(exitLatch::countDown);

        Thread pollThread = new Thread(runner::pollAndExecute);
        pollThread.setDaemon(true);
        pollThread.start();

        assertTrue(exitLatch.await(10, TimeUnit.SECONDS),
                "exitAction must be invoked when a FatalAuthenticationException propagates through the poll loop");
    }
}
