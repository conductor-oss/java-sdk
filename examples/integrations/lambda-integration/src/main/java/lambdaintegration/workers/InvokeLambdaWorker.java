package lambdaintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Invokes an AWS Lambda function.
 * Input: functionName, qualifier, payload
 * Output: statusCode, responsePayload, duration, requestId, billedDuration
 */
public class InvokeLambdaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lam_invoke";
    }

    @Override
    public TaskResult execute(Task task) {
        String functionName = (String) task.getInputData().get("functionName");
        String qualifier = (String) task.getInputData().get("qualifier");
        int duration = 250;

        java.util.Map<String, Object> responsePayload = java.util.Map.of(
                "statusCode", 200,
                "body", java.util.Map.of("message", "Processed successfully", "itemCount", 42));
        System.out.println("  [invoke] " + functionName + ":" + qualifier + " -> 200 (" + duration + "ms)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("statusCode", 200);
        result.getOutputData().put("responsePayload", responsePayload);
        result.getOutputData().put("duration", duration);
        result.getOutputData().put("requestId", "req-" + Long.toString(System.currentTimeMillis(), 36));
        result.getOutputData().put("billedDuration", duration + 50);
        return result;
    }
}
