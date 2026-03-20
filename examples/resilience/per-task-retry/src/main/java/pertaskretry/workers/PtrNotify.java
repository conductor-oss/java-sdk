package pertaskretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends notification for an order. Configured with retryCount:3, FIXED retry, 2s delay.
 * Returns { result: "email_sent", orderId: ... }
 */
public class PtrNotify implements Worker {

    @Override
    public String getTaskDefName() {
        return "ptr_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");

        System.out.println("[ptr_notify] Sending notification for order: " + orderId);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", "email_sent");
        output.put("orderId", orderId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);

        System.out.println("  Result: email_sent");
        return result;
    }
}
