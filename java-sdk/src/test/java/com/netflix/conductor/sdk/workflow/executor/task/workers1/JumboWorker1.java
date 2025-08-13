package com.netflix.conductor.sdk.workflow.executor.task.workers1;


import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

public class JumboWorker1 {
    @WorkerTask("jumbo_task_1_1")
    public @OutputParam("result") String task1() {
        return "dummy data 1";
    }

    @WorkerTask("jumbo_task_1_2")
    public @OutputParam("result") String task2() {
        return "dummy data 2";
    }

    @WorkerTask("jumbo_task_1_3")
    public @OutputParam("result") String task3() {
        return "dummy data 3";
    }

    @WorkerTask("jumbo_task_1_4")
    public @OutputParam("result") String task4() {
        return "dummy data 4";
    }

    @WorkerTask("jumbo_task_1_5")
    public @OutputParam("result") String task5() {
        return "dummy data 5";
    }

    @WorkerTask("jumbo_task_1_6")
    public @OutputParam("result") String task6() {
        return "dummy data 6";
    }

    @WorkerTask("jumbo_task_1_7")
    public @OutputParam("result") String task7() {
        return "dummy data 7";
    }
}
