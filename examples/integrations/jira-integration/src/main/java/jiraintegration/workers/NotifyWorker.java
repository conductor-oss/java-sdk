package jiraintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Notifies the assignee about a Jira issue status change.
 * Input: issueKey, assignee, newStatus
 * Output: notified
 */
public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jra_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String issueKey = (String) task.getInputData().get("issueKey");
        if (issueKey == null) {
            issueKey = "UNKNOWN-0";
        }
        String assignee = (String) task.getInputData().get("assignee");
        if (assignee == null) {
            assignee = "unassigned";
        }
        String newStatus = (String) task.getInputData().get("newStatus");
        if (newStatus == null) {
            newStatus = "Unknown";
        }

        System.out.println("  [notify] Notified " + assignee + " about " + issueKey + " -> " + newStatus);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("status", "notified");
        return result;
    }
}
