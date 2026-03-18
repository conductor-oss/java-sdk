package deadlinemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

public class HandleUrgentWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "ded_handle_urgent";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String taskId = (String) task.getInputData().getOrDefault("taskId", "unknown");
        String projectId = (String) task.getInputData().getOrDefault("projectId", "unknown");
        long hoursRemaining = toLong(task.getInputData().getOrDefault("hoursRemaining", 0));
        String dueDate = (String) task.getInputData().getOrDefault("dueDate", "unknown");

        // Determine escalation level based on hours remaining
        String priority;
        String assignedTo;
        String escalationLevel;
        String nextCheckIn;

        if (hoursRemaining <= 4) {
            priority = "P0";
            assignedTo = "senior-team-lead";
            escalationLevel = "critical";
            nextCheckIn = "1h";
        } else if (hoursRemaining <= 8) {
            priority = "P1";
            assignedTo = "senior-team";
            escalationLevel = "high";
            nextCheckIn = "2h";
        } else if (hoursRemaining <= 16) {
            priority = "P1";
            assignedTo = "team-lead";
            escalationLevel = "elevated";
            nextCheckIn = "4h";
        } else {
            priority = "P2";
            assignedTo = "team";
            escalationLevel = "moderate";
            nextCheckIn = "6h";
        }

        // Build notification details
        String notificationMessage = String.format(
                "URGENT: Task %s (project %s) has %dh remaining before deadline %s. "
                        + "Escalated to %s at %s priority.",
                taskId, projectId, hoursRemaining, dueDate, assignedTo, priority
        );

        System.out.println("  [urgent] " + notificationMessage);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("taskId", taskId);
        r.getOutputData().put("projectId", projectId);
        r.getOutputData().put("escalated", true);
        r.getOutputData().put("assignedTo", assignedTo);
        r.getOutputData().put("priority", priority);
        r.getOutputData().put("escalationLevel", escalationLevel);
        r.getOutputData().put("hoursRemaining", hoursRemaining);
        r.getOutputData().put("dueDate", dueDate);
        r.getOutputData().put("nextCheckIn", nextCheckIn);
        r.getOutputData().put("notificationMessage", notificationMessage);
        r.getOutputData().put("escalatedAt", Instant.now().toString());
        return r;
    }

    private static long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}
