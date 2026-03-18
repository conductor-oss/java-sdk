package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Checks if current failures exceed the escalation threshold.
 * Escalates when the number of failing endpoints meets or exceeds the threshold.
 *
 * In production, you could also query a state store (Redis/DynamoDB) for
 * historical failure counts across multiple monitor runs.
 */
public class CheckEscalation implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_check_escalation";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_check_escalation] Checking escalation threshold...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> failures = (List<Map<String, Object>>) task.getInputData().get("failures");
        int threshold = toInt(task.getInputData().get("threshold"), 3);

        int failureCount = failures != null ? failures.size() : 0;
        boolean shouldEscalate = failureCount >= threshold;

        String reason = shouldEscalate
                ? failureCount + " failing endpoint(s) >= threshold " + threshold
                : failureCount + " failing endpoint(s) < threshold " + threshold;

        System.out.println("  Failing endpoints: " + failureCount + " (threshold: " + threshold + ")");
        System.out.println("  Escalate: " + shouldEscalate);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("shouldEscalate", shouldEscalate);
        result.getOutputData().put("failureCount", failureCount);
        result.getOutputData().put("threshold", threshold);
        result.getOutputData().put("reason", reason);
        return result;
    }

    private int toInt(Object val, int def) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return def; }
    }
}
