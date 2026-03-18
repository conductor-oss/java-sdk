package batchscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Executes the batch with allocated resources.
 * Input: batchId, allocation, orderedJobs
 * Output: jobsCompleted, jobsFailed, totalDurationMs, results
 */
public class ExecuteBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bs_execute_batch";
    }

    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");

        System.out.println("  [execute] Executing batch " + batchId + " with allocated resources...");

        List<Map<String, Object>> results = List.of(
                Map.of("job", "etl-import", "status", "success"),
                Map.of("job", "data-transform", "status", "success"),
                Map.of("job", "report-gen", "status", "success"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("jobsCompleted", 3);
        result.getOutputData().put("jobsFailed", 0);
        result.getOutputData().put("totalDurationMs", 11200);
        result.getOutputData().put("results", results);
        return result;
    }
}
