package escalationtimer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for et_process -- processes the approval decision.
 *
 * Reads "decision" and "method" from input, logs them, and returns { processed: true }.
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "et_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String decision = "unknown";
        String method = "unknown";

        Object decisionInput = task.getInputData().get("decision");
        if (decisionInput instanceof String) {
            decision = (String) decisionInput;
        }

        Object methodInput = task.getInputData().get("method");
        if (methodInput instanceof String) {
            method = (String) methodInput;
        }

        System.out.println("  [et_process] Decision: " + decision + " (" + method + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);

        return result;
    }
}
