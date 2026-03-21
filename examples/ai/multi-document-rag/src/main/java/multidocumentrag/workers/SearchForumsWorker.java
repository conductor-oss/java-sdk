package multidocumentrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the forums collection and returns 1 result.
 */
public class SearchForumsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mdrag_search_forums";
    }

    @Override
    public TaskResult execute(Task task) {
        String collection = (String) task.getInputData().get("collection");
        System.out.println("  [search] Querying collection: \"" + collection + "\"");

        List<Map<String, Object>> results = List.of(
                Map.of("text", "Community answer: Conductor handles retries automatically when workers fail.",
                        "source", "forums", "score", 0.90)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
