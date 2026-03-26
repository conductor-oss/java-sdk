package workflowdebugging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects trace data from an instrumented execution.
 * Input: executionId, tracePoints
 * Output: traceData
 */
public class WfdCollectTraceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfd_collect_trace";
    }

    @Override
    public TaskResult execute(Task task) {
        String executionId = (String) task.getInputData().getOrDefault("executionId", "unknown");

        System.out.println("  [collect] Collecting trace data for execution \"" + executionId + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("traceData", List.of(
                Map.of("tracePointId", "tp-1", "timestamp", System.currentTimeMillis() - 3000, "value", 12),
                Map.of("tracePointId", "tp-2", "timestamp", System.currentTimeMillis() - 1500, "value", 28),
                Map.of("tracePointId", "tp-3", "timestamp", System.currentTimeMillis(), "value", "branch-A")));
        return result;
    }
}
