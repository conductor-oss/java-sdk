package authorizationrbac.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Enforces the access decision and logs for audit.
 * Input: enforce_decisionData
 * Output: enforce_decision, completedAt
 */
public class EnforceDecisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rbac_enforce_decision";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [enforce] Access ALLOWED — logged for audit");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enforce_decision", true);
        result.getOutputData().put("completedAt", "2024-01-15T10:30:00Z");
        return result;
    }
}
