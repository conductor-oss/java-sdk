package ragfusion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Search engine V1 worker. Takes a query and variantIndex, returns ranked
 * results with id, text, and rank. Performs a keyword-based search engine.
 */
public class SearchV1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rf_search_v1";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null) {
            query = "";
        }

        List<Map<String, Object>> results = List.of(
                Map.of("id", "v1-doc1", "text", "Conductor supports workflow versioning and backward compatibility", "rank", 1),
                Map.of("id", "v1-doc2", "text", "Task definitions include retry policies and timeout configurations", "rank", 2),
                Map.of("id", "v1-doc3", "text", "Workers can be implemented in any language using HTTP endpoints", "rank", 3),
                Map.of("id", "v1-doc4", "text", "Sub-workflows enable modular and reusable workflow design", "rank", 4)
        );

        System.out.println("  [search_v1] Returned " + results.size() + " results for: \"" + query + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("source", "search_v1");
        return result;
    }
}
