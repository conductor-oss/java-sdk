package elasticsearchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Aggregates search results.
 * Input: indexName, hits
 * Output: aggregations, avgScore, topCategory
 */
public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "els_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Top category: analytics, avg score: 8.27");

        java.util.Map<String, Object> aggregations = java.util.Map.of(
                "categories", java.util.Map.of("analytics", 2, "sales", 1),
                "avgScore", 8.27);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregations", aggregations);
        result.getOutputData().put("avgScore", 8.27);
        result.getOutputData().put("topCategory", "analytics");
        return result;
    }
}
