/*
 * Copyright 2025 Conductor Authors.
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
package com.netflix.conductor.sdk.workflow.executor.task.workers2;


import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

public class JumboWorker2 {
    @WorkerTask("jumbo_task_2_1")
    public @OutputParam("result") String task1() {
        return "dummy data 1";
    }

    @WorkerTask("jumbo_task_2_2")
    public @OutputParam("result") String task2() {
        return "dummy data 2";
    }

    @WorkerTask("jumbo_task_2_3")
    public @OutputParam("result") String task3() {
        return "dummy data 3";
    }

    @WorkerTask("jumbo_task_2_4")
    public @OutputParam("result") String task4() {
        return "dummy data 4";
    }

    @WorkerTask("jumbo_task_2_5")
    public @OutputParam("result") String task5() {
        return "dummy data 5";
    }

    @WorkerTask("jumbo_task_2_6")
    public @OutputParam("result") String task6() {
        return "dummy data 6";
    }

    @WorkerTask("jumbo_task_2_7")
    public @OutputParam("result") String task7() {
        return "dummy data 7";
    }
}
