package multidocumentrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the API docs collection and returns 2 results.
 */
public class SearchApiDocsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mdrag_search_api_docs";
    }

    @Override
    public TaskResult execute(Task task) {
        String collection = (String) task.getInputData().get("collection");
        System.out.println("  [search] Querying collection: \"" + collection + "\"");

        List<Map<String, Object>> results = List.of(
                Map.of("text", "The /workflow endpoint accepts POST requests with workflow input as JSON body.",
                        "source", "api_docs", "score", 0.95),
                Map.of("text", "Task definitions must be registered before use with POST /metadata/taskdefs.",
                        "source", "api_docs", "score", 0.88)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
