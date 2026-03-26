package chaininghttptasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that prepares an HTTP request by extracting the search term from the query input.
 * Runs before the HTTP system task so the workflow can pass the prepared data into the HTTP call.
 */
public class PrepareRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "http_prepare_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "default";
        }

        System.out.println("  [prepare] Building search for: " + query);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("searchTerm", query);
        return result;
    }
}
