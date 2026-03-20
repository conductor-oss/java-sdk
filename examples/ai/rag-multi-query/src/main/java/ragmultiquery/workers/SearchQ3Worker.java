package ragmultiquery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the knowledge base with query variant 3.
 * Returns documents d4 and d11 (d4 overlaps with q1).
 */
public class SearchQ3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mq_search_q3";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        System.out.println("  [search-3] \"" + query + "\"");

        List<Map<String, String>> results = List.of(
                Map.of("id", "d4", "text", "Orchestration enables retry, timeout, and compensation logic."),
                Map.of("id", "d11", "text", "Choreography can lead to hidden dependencies and hard-to-debug flows.")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
