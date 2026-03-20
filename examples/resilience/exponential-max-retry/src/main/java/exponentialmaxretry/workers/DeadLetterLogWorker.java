package exponentialmaxretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for emr_dead_letter_log — logs details of a failed workflow.
 *
 * Receives failed workflow details (workflowId, reason, etc.) as input
 * and logs them. Returns logged=true on completion.
 */
public class DeadLetterLogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_dead_letter_log";
    }

    @Override
    public TaskResult execute(Task task) {
        String failedWorkflowId = task.getInputData().get("failedWorkflowId") != null
                ? String.valueOf(task.getInputData().get("failedWorkflowId"))
                : "unknown";
        String reason = task.getInputData().get("reason") != null
                ? String.valueOf(task.getInputData().get("reason"))
                : "unknown";

        System.out.println("  [emr_dead_letter_log] Logging failed workflow:");
        System.out.println("    Workflow ID: " + failedWorkflowId);
        System.out.println("    Reason: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("failedWorkflowId", failedWorkflowId);
        result.getOutputData().put("reason", reason);

        return result;
    }
}
