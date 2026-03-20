package webhooksecurity.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies a webhook signature by comparing expected vs provided values.
 * Input: expected, provided
 * Output: result ("valid" or "invalid"), match (boolean)
 */
public class VerifySignatureWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ws_verify_signature";
    }

    @Override
    public TaskResult execute(Task task) {
        String expected = (String) task.getInputData().get("expected");
        String provided = (String) task.getInputData().get("provided");

        if (expected == null) {
            expected = "";
        }
        if (provided == null) {
            provided = "";
        }

        System.out.println("  [ws_verify_signature] Comparing signatures...");

        boolean match = expected.equals(provided);
        String resultValue = match ? "valid" : "invalid";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", resultValue);
        result.getOutputData().put("match", match);
        return result;
    }
}
