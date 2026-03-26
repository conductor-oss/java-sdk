package ragmultiquery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the knowledge base with query variant 2.
 * Returns documents d1, d7, d9 (d1 overlaps with q1).
 */
public class SearchQ2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mq_search_q2";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        System.out.println("  [search-2] \"" + query + "\"");

        List<Map<String, String>> results = List.of(
                Map.of("id", "d1", "text", "Workflow orchestration provides centralized control and visibility."),
                Map.of("id", "d7", "text", "Conductor decouples task execution from workflow logic."),
                Map.of("id", "d9", "text", "Conductor supports versioned workflows for safe deployments.")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
