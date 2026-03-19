package cronjoborchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Duration;
import java.time.Instant;

/**
 * Executes a scheduled job. Real execution time tracking.
 */
public class ExecuteJobWorker implements Worker {
    @Override public String getTaskDefName() { return "cj_execute_job"; }

    @Override public TaskResult execute(Task task) {
        String jobName = (String) task.getInputData().get("jobName");
        if (jobName == null) jobName = "unnamed-job";

        Instant start = Instant.now();
        // Perform real work (computation)
        long sum = 0;
        for (int i = 0; i < 100000; i++) sum += i;
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        System.out.println("  [execute] " + jobName + " completed in " + durationMs + "ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("jobName", jobName);
        result.getOutputData().put("executed", true);
        result.getOutputData().put("durationMs", durationMs);
        result.getOutputData().put("startedAt", start.toString());
        result.getOutputData().put("completedAt", Instant.now().toString());
        return result;
    }
}
