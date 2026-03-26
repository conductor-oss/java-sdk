package pertaskretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Processes payment for an order. Configured with retryCount:5, EXPONENTIAL_BACKOFF, 1s base delay.
 * Returns { result: "charged", attempt: N }
 */
public class PtrPayment implements Worker {

    @Override
    public String getTaskDefName() {
        return "ptr_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        int attempt = task.getRetryCount() + 1;

        System.out.println("[ptr_payment] Processing payment for order: " + orderId
                + " (attempt " + attempt + ")");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", "charged");
        output.put("attempt", attempt);
        output.put("orderId", orderId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);

        System.out.println("  Result: charged (attempt " + attempt + ")");
        return result;
    }
}
