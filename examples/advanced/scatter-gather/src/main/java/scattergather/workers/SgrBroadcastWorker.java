package scattergather.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Broadcasts the query to all configured sources. Validates the query
 * and sources, and prepares metadata for the gather workers.
 *
 * Input: query (String), sources (list of source names)
 * Output: broadcasted (boolean), sourceCount (int), validatedQuery (String)
 */
public class SgrBroadcastWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgr_broadcast";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().getOrDefault("query", "");
        Object sourcesObj = task.getInputData().get("sources");

        List<String> sources = new ArrayList<>();
        if (sourcesObj instanceof List) {
            for (Object s : (List<?>) sourcesObj) {
                sources.add(s != null ? s.toString() : "unknown");
            }
        }

        // Validate and normalize the query
        String validatedQuery = query.trim().toLowerCase();
        int sourceCount = sources.isEmpty() ? 3 : sources.size(); // default to 3 sources

        System.out.println("  [broadcast] Broadcasting query \"" + validatedQuery
                + "\" to " + sourceCount + " sources");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("broadcasted", true);
        result.getOutputData().put("sourceCount", sourceCount);
        result.getOutputData().put("validatedQuery", validatedQuery);
        return result;
    }
}
