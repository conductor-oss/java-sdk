package ragfusion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Search engine V2 worker. Takes a query and variantIndex, returns ranked
 * results with id, text, and rank. Performs a semantic search engine.
 */
public class SearchV2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rf_search_v2";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null) {
            query = "";
        }

        List<Map<String, Object>> results = List.of(
                Map.of("id", "v2-doc1", "text", "JSON-based input and output enables flexible data flow between tasks", "rank", 1),
                Map.of("id", "v2-doc2", "text", "Conductor supports workflow versioning and backward compatibility", "rank", 2),
                Map.of("id", "v2-doc3", "text", "Event-driven workflows respond to external signals and webhooks", "rank", 3),
                Map.of("id", "v2-doc4", "text", "Dynamic forking allows parallel execution of variable task sets", "rank", 4)
        );

        System.out.println("  [search_v2] Returned " + results.size() + " results for: \"" + query + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("source", "search_v2");
        return result;
    }
}
