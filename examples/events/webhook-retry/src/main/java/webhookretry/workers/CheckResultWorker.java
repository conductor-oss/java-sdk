package webhookretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks the result of a webhook delivery attempt.
 * Input: statusCode, attempt
 * Output: success (true if statusCode == 200), statusCode
 */
public class CheckResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wr_check_result";
    }

    @Override
    public TaskResult execute(Task task) {
        Object statusCodeRaw = task.getInputData().get("statusCode");
        int statusCode = 500;
        if (statusCodeRaw instanceof Number) {
            statusCode = ((Number) statusCodeRaw).intValue();
        }

        boolean success = statusCode == 200;

        System.out.println("  [wr_check_result] Status " + statusCode + " -> success=" + success);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("success", success);
        result.getOutputData().put("statusCode", statusCode);
        return result;
    }
}
