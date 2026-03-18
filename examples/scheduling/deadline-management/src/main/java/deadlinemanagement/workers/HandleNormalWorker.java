package deadlinemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class HandleNormalWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "ded_handle_normal";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String taskId = (String) task.getInputData().getOrDefault("taskId", "unknown");
        String projectId = (String) task.getInputData().getOrDefault("projectId", "unknown");
        long hoursRemaining = toLong(task.getInputData().getOrDefault("hoursRemaining", 168));
        String dueDate = (String) task.getInputData().getOrDefault("dueDate", "unknown");
        double percentTimeRemaining = toDouble(task.getInputData().getOrDefault("percentTimeRemaining", 100));

        // Determine appropriate check-in interval based on remaining time
        String nextCheckIn;
        String action;
        if (hoursRemaining > 72) {
            nextCheckIn = "48h";
            action = "monitor";
        } else if (hoursRemaining > 48) {
            nextCheckIn = "24h";
            action = "monitor";
        } else {
            // Getting closer, check more frequently
            nextCheckIn = "12h";
            action = "monitor_closely";
        }

        Instant nextCheckTime = Instant.now().plus(parseHours(nextCheckIn), ChronoUnit.HOURS);

        // Compute a progress assessment
        String progressStatus;
        if (percentTimeRemaining > 60) {
            progressStatus = "on_track";
        } else if (percentTimeRemaining > 30) {
            progressStatus = "needs_attention";
        } else {
            progressStatus = "at_risk";
        }

        System.out.println("  [normal] Task " + taskId + " (project=" + projectId + "): "
                + progressStatus + ", " + hoursRemaining + "h remaining, next check in " + nextCheckIn);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("taskId", taskId);
        r.getOutputData().put("projectId", projectId);
        r.getOutputData().put("action", action);
        r.getOutputData().put("nextCheckIn", nextCheckIn);
        r.getOutputData().put("nextCheckTime", nextCheckTime.toString());
        r.getOutputData().put("progressStatus", progressStatus);
        r.getOutputData().put("hoursRemaining", hoursRemaining);
        r.getOutputData().put("dueDate", dueDate);
        r.getOutputData().put("processedAt", Instant.now().toString());
        return r;
    }

    private static long parseHours(String interval) {
        return Long.parseLong(interval.replace("h", ""));
    }

    private static long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(String.valueOf(obj)); } catch (Exception e) { return 168; }
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }
}
