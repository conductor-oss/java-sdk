package apmworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Collects application traces for the specified service and time range.
 */
public class CollectTraces implements Worker {

    @Override
    public String getTaskDefName() {
        return "apm_collect_traces";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        String timeRange = (String) task.getInputData().get("timeRange");

        System.out.println("[apm_collect_traces] Collecting traces for " + serviceName + " (" + timeRange + ")...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("traceCount", 25000);
        result.getOutputData().put("sampledTraces", 2500);
        result.getOutputData().put("timeRange", timeRange);
        return result;
    }
}
