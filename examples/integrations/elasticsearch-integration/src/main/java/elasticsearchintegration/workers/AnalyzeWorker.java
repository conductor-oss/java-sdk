package elasticsearchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Analyzes aggregated results.
 * Input: aggregations, totalHits
 * Output: insights, analyzedAt
 */
public class AnalyzeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "els_analyze";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalHits = task.getInputData().get("totalHits");
        String insights = "Found " + totalHits + " documents. Top category: analytics.";
        System.out.println("  [analyze] " + insights);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("insights", insights);
        result.getOutputData().put("analyzedAt", java.time.Instant.now().toString());
        return result;
    }
}
