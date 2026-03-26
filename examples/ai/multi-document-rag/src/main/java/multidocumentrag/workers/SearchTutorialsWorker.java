package multidocumentrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the tutorials collection and returns 2 results.
 */
public class SearchTutorialsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mdrag_search_tutorials";
    }

    @Override
    public TaskResult execute(Task task) {
        String collection = (String) task.getInputData().get("collection");
        System.out.println("  [search] Querying collection: \"" + collection + "\"");

        List<Map<String, Object>> results = List.of(
                Map.of("text", "Tutorial: Building your first Conductor workflow with Node.js workers in 10 minutes.",
                        "source", "tutorials", "score", 0.92),
                Map.of("text", "Tutorial: Using FORK/JOIN for parallel task execution in workflow definitions.",
                        "source", "tutorials", "score", 0.85)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
