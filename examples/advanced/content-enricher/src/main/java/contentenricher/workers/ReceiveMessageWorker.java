package contentenricher.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming message and extracts the customer ID.
 * Input: message
 * Output: message, customerId
 */
public class ReceiveMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "enr_receive_message";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object msgObj = task.getInputData().get("message");
        Map<String, Object> msg = (msgObj instanceof Map) ? (Map<String, Object>) msgObj : Map.of();

        String customerId = msg.containsKey("customerId") ? String.valueOf(msg.get("customerId")) : "unknown";

        System.out.println("  [receive] Message received — customer: " + customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("message", msg);
        result.getOutputData().put("customerId", customerId);
        return result;
    }
}
