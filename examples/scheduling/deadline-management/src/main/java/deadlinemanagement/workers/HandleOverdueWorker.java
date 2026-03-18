package deadlinemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

public class HandleOverdueWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "ded_handle_overdue";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String taskId = (String) task.getInputData().getOrDefault("taskId", "unknown");
        String projectId = (String) task.getInputData().getOrDefault("projectId", "unknown");
        long hoursOverdue = toLong(task.getInputData().getOrDefault("hoursOverdue", 0));
        String dueDate = (String) task.getInputData().getOrDefault("dueDate", "unknown");

        // Calculate penalty based on hours overdue
        // Penalty structure: $50 base + $25 per hour overdue, capped at $5000
        double basePenalty = 50.0;
        double perHourPenalty = 25.0;
        double maxPenalty = 5000.0;
        double penalty = Math.min(basePenalty + (hoursOverdue * perHourPenalty), maxPenalty);
        penalty = Math.round(penalty * 100.0) / 100.0;

        // Determine escalation level based on how overdue
        String priority;
        String assignedTo;
        String escalationLevel;

        if (hoursOverdue >= 72) {
            priority = "P0";
            assignedTo = "executive-leadership";
            escalationLevel = "executive";
        } else if (hoursOverdue >= 24) {
            priority = "P0";
            assignedTo = "director";
            escalationLevel = "director";
        } else if (hoursOverdue >= 8) {
            priority = "P0";
            assignedTo = "management";
            escalationLevel = "management";
        } else {
            priority = "P0";
            assignedTo = "senior-team-lead";
            escalationLevel = "senior";
        }

        // Impact assessment
        String impact;
        if (hoursOverdue >= 48) {
            impact = "severe";
        } else if (hoursOverdue >= 24) {
            impact = "high";
        } else if (hoursOverdue >= 8) {
            impact = "moderate";
        } else {
            impact = "low";
        }

        String notificationMessage = String.format(
                "OVERDUE: Task %s (project %s) is %dh past deadline %s. "
                        + "Penalty: $%.2f. Escalated to %s. Impact: %s.",
                taskId, projectId, hoursOverdue, dueDate, penalty, assignedTo, impact
        );

        System.out.println("  [overdue] " + notificationMessage);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("taskId", taskId);
        r.getOutputData().put("projectId", projectId);
        r.getOutputData().put("escalated", true);
        r.getOutputData().put("assignedTo", assignedTo);
        r.getOutputData().put("priority", priority);
        r.getOutputData().put("escalationLevel", escalationLevel);
        r.getOutputData().put("hoursOverdue", hoursOverdue);
        r.getOutputData().put("penalty", penalty);
        r.getOutputData().put("impact", impact);
        r.getOutputData().put("dueDate", dueDate);
        r.getOutputData().put("notificationMessage", notificationMessage);
        r.getOutputData().put("processedAt", Instant.now().toString());
        return r;
    }

    private static long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}
