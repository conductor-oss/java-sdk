package errorclassification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ec_api_call — performs different HTTP error codes based on
 * the triggerError input parameter.
 *
 * Behavior by triggerError value:
 *   "security-posture" → COMPLETED with errorType="non_retryable", error message, httpStatus=400
 *   "429" → FAILED (retryable — Conductor retries automatically)
 *   "503" → FAILED (retryable — Conductor retries automatically)
 *   default/none → COMPLETED with result="success", errorType="none"
 */
public class ApiCallWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_api_call";
    }

    @Override
    public TaskResult execute(Task task) {
        Object triggerErrorInput = task.getInputData().get("triggerError");
        String triggerError = triggerErrorInput != null ? triggerErrorInput.toString() : "";

        System.out.println("  [ec_api_call] triggerError=" + triggerError);

        TaskResult result = new TaskResult(task);

        switch (triggerError) {
            case "security-posture":
                System.out.println("  [ec_api_call] Non-retryable error: security-posture Bad Request");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("errorType", "non_retryable");
                result.getOutputData().put("error", "Bad Request: invalid input parameters");
                result.getOutputData().put("httpStatus", 400);
                break;

            case "429":
                System.out.println("  [ec_api_call] Retryable error: 429 Too Many Requests");
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", "Too Many Requests: rate limit exceeded");
                result.getOutputData().put("httpStatus", 429);
                break;

            case "503":
                System.out.println("  [ec_api_call] Retryable error: 503 Service Unavailable");
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", "Service Unavailable: try again later");
                result.getOutputData().put("httpStatus", 503);
                break;

            default:
                System.out.println("  [ec_api_call] Success — no error deterministic.");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("result", "success");
                result.getOutputData().put("errorType", "none");
                break;
        }

        return result;
    }
}
