package jiraintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Transitions a Jira issue from one status to another.
 * Input: issueKey, fromStatus, toStatus
 * Output: newStatus, transitionedAt
 */
public class TransitionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jra_transition";
    }

    @Override
    public TaskResult execute(Task task) {
        String issueKey = (String) task.getInputData().get("issueKey");
        if (issueKey == null) {
            issueKey = "UNKNOWN-0";
        }
        String fromStatus = (String) task.getInputData().get("fromStatus");
        if (fromStatus == null) {
            fromStatus = "Unknown";
        }
        String toStatus = (String) task.getInputData().get("toStatus");
        if (toStatus == null) {
            toStatus = "In Progress";
        }

        System.out.println("  [transition] " + issueKey + ": " + fromStatus + " -> " + toStatus);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("newStatus", toStatus);
        result.getOutputData().put("transitionedAt", "2025-01-15T10:30:02Z");
        return result;
    }
}
