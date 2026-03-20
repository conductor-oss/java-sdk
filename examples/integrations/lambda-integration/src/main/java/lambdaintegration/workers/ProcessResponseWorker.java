package lambdaintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a Lambda response.
 * Input: statusCode, responsePayload
 * Output: result, success
 */
public class ProcessResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lam_process_response";
    }

    @Override
    public TaskResult execute(Task task) {
        Object statusCode = task.getInputData().get("statusCode");
        boolean success = Integer.valueOf(200).equals(statusCode);
        Object responsePayload = task.getInputData().get("responsePayload");
        System.out.println("  [process] Status: " + statusCode + ", Success: " + success);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", success ? responsePayload : java.util.Map.of("error", "Invocation failed"));
        result.getOutputData().put("success", success);
        return result;
    }
}
