package servicediscovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Calls the selected service instance.
 * Input: instance, request
 * Output: response, latency, success
 */
public class CallServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_call_service";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> instance = (Map<String, Object>) task.getInputData().get("instance");
        String host = instance != null ? (String) instance.get("host") : "unknown";
        Object port = instance != null ? instance.get("port") : 0;

        System.out.println("  [sd_call_service] Calling " + host + ":" + port + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", Map.of("data", Map.of("orderId", "ORD-555", "status", "processed")));
        result.getOutputData().put("latency", 23);
        result.getOutputData().put("success", true);
        return result;
    }
}
