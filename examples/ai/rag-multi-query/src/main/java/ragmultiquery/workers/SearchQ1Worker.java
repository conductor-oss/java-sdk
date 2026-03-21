package ragmultiquery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the knowledge base with query variant 1.
 * Returns documents d1 and d4 (d1 overlaps with q2, d4 overlaps with q3).
 */
public class SearchQ1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mq_search_q1";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        System.out.println("  [search-1] \"" + query + "\"");

        List<Map<String, String>> results = List.of(
                Map.of("id", "d1", "text", "Workflow orchestration provides centralized control and visibility."),
                Map.of("id", "d4", "text", "Orchestration enables retry, timeout, and compensation logic.")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
