package lambdaintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Prepares a Lambda invocation payload.
 * Input: functionName, inputData
 * Output: payload
 */
public class PreparePayloadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lam_prepare_payload";
    }

    @Override
    public TaskResult execute(Task task) {
        String functionName = (String) task.getInputData().get("functionName");
        Object inputData = task.getInputData().get("inputData");
        System.out.println("  [prepare] Payload for " + functionName + ": " + inputData);

        java.util.Map<String, Object> payload = java.util.Map.of(
                "body", String.valueOf(inputData),
                "headers", java.util.Map.of("Content-Type", "application/json"),
                "requestContext", java.util.Map.of("requestId", "req-" + System.currentTimeMillis()));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("payload", payload);
        return result;
    }
}
