package apigateway.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates an API key and returns client info.
 * Input: apiKey
 * Output: clientId, tier, rateLimit
 */
public class AgAuthenticateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ag_authenticate";
    }

    @Override
    public TaskResult execute(Task task) {
        String apiKey = (String) task.getInputData().get("apiKey");
        if (apiKey == null) {
            apiKey = "unknown";
        }

        System.out.println("  [ag_authenticate] Validating API key " + apiKey + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("clientId", "client-enterprise-01");
        result.getOutputData().put("tier", "premium");
        result.getOutputData().put("rateLimit", 10000);
        return result;
    }
}
