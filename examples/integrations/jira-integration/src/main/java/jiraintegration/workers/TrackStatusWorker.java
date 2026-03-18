package jiraintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Tracks the current status of a Jira issue.
 * Input: issueKey
 * Output: currentStatus, lastUpdated
 */
public class TrackStatusWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jra_track_status";
    }

    @Override
    public TaskResult execute(Task task) {
        String issueKey = (String) task.getInputData().get("issueKey");
        if (issueKey == null) {
            issueKey = "UNKNOWN-0";
        }

        System.out.println("  [track] Issue " + issueKey + " status: To Do");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("currentStatus", "To Do");
        result.getOutputData().put("lastUpdated", "2025-01-15T10:30:01Z");
        return result;
    }
}
