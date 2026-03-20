package featureflagrollout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class MonitorImpactWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_monitor_impact"; }

    @Override
    public TaskResult execute(Task task) {
        String flagName = (String) task.getInputData().get("flagName");
        if (flagName == null) flagName = "unknown-flag";
        String duration = (String) task.getInputData().get("duration");
        if (duration == null) duration = "10m";

        System.out.println("  [monitor] Impact: conversion +2.3%, errors stable (duration=" + duration + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("healthy", true);
        result.getOutputData().put("metrics", Map.of("conversionDelta", 2.3, "errorDelta", 0));
        result.getOutputData().put("duration", duration);
        return result;
    }
}
