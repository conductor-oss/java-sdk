package cronjoborchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Logs job execution result.
 */
public class LogResultWorker implements Worker {
    @Override public String getTaskDefName() { return "cj_log_result"; }

    @Override public TaskResult execute(Task task) {
        String jobName = (String) task.getInputData().get("jobName");
        Object durationObj = task.getInputData().get("durationMs");
        Object executedObj = task.getInputData().get("executed");
        if (jobName == null) jobName = "unnamed";

        boolean executed = Boolean.TRUE.equals(executedObj);
        long duration = durationObj instanceof Number ? ((Number) durationObj).longValue() : 0;

        String logEntry = String.format("[%s] Job '%s' %s in %dms",
                Instant.now(), jobName, executed ? "completed" : "failed", duration);

        System.out.println("  [log] " + logEntry);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("logEntry", logEntry);
        return result;
    }
}
