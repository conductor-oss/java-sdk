package selfhealing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sh_retry_process — retries processing after remediation.
 *
 * Returns result="healed-{data}" to indicate the service was healed
 * and processing succeeded after remediation.
 */
public class RetryProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_retry_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput != null) {
            data = dataInput.toString();
        }

        System.out.println("  [sh_retry_process] Retry processing data after healing: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "healed-" + data);

        return result;
    }
}
