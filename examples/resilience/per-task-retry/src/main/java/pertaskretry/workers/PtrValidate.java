package pertaskretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates an order. Configured with retryCount:1, FIXED retry, 1s delay.
 * Returns { result: "valid", orderId: ... }
 */
public class PtrValidate implements Worker {

    @Override
    public String getTaskDefName() {
        return "ptr_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");

        System.out.println("[ptr_validate] Validating order: " + orderId);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", "valid");
        output.put("orderId", orderId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);

        System.out.println("  Result: valid");
        return result;
    }
}
