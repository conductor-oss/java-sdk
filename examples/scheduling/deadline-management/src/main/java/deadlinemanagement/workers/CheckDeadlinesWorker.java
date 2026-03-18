package deadlinemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class CheckDeadlinesWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "ded_check_deadlines";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String taskId = (String) task.getInputData().getOrDefault("taskId", "unknown");
        String dueDateStr = (String) task.getInputData().getOrDefault("dueDate", null);
        String projectId = (String) task.getInputData().getOrDefault("projectId", "unknown");

        if (dueDateStr == null || dueDateStr.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required field: dueDate");
            return r;
        }

        Instant dueDate;
        try {
            dueDate = Instant.parse(dueDateStr);
        } catch (DateTimeParseException e) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Invalid dueDate format (expected ISO-8601): " + dueDateStr);
            return r;
        }

        Instant now = Instant.now();
        Duration remaining = Duration.between(now, dueDate);
        long hoursRemaining = remaining.toHours();
        long hoursOverdue = 0;

        // Determine urgency
        String urgency;
        if (remaining.isNegative()) {
            // Overdue
            urgency = "overdue";
            hoursOverdue = Math.abs(hoursRemaining);
            hoursRemaining = 0;
        } else if (hoursRemaining <= 24) {
            // Less than 24 hours remaining -> urgent
            urgency = "urgent";
        } else {
            // More than 24 hours -> normal
            urgency = "normal";
        }

        double percentTimeRemaining = 0;
        // Estimate percent of time remaining based on a default 7-day project cycle
        long totalHoursEstimate = 7 * 24; // assume 7-day deadline window
        if (hoursRemaining > 0) {
            percentTimeRemaining = Math.min(100, Math.round((hoursRemaining * 100.0) / totalHoursEstimate * 10.0) / 10.0);
        }

        System.out.println("  [check] Task " + taskId + " (project=" + projectId + "): urgency="
                + urgency + ", hoursRemaining=" + hoursRemaining
                + ", hoursOverdue=" + hoursOverdue + ", due=" + dueDateStr);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("taskId", taskId);
        r.getOutputData().put("projectId", projectId);
        r.getOutputData().put("dueDate", dueDateStr);
        r.getOutputData().put("urgency", urgency);
        r.getOutputData().put("hoursRemaining", hoursRemaining);
        r.getOutputData().put("hoursOverdue", hoursOverdue);
        r.getOutputData().put("percentTimeRemaining", percentTimeRemaining);
        r.getOutputData().put("checkedAt", now.toString());
        return r;
    }
}
