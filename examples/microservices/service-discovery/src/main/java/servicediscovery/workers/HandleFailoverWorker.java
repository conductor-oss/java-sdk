package servicediscovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Handles failover if the service call failed.
 * Input: response, instance, allInstances
 * Output: finalResult, failoverTriggered
 */
public class HandleFailoverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_handle_failover";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> response = (Map<String, Object>) task.getInputData().get("response");
        boolean success = response != null && response.get("data") != null;

        System.out.println("  [sd_handle_failover] Call " + (success ? "succeeded" : "failed") + ", failover " + (success ? "not needed" : "triggered") + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalResult", success ? response.get("data") : Map.of());
        result.getOutputData().put("failoverTriggered", !success);
        return result;
    }
}
