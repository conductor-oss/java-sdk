package cronjoborchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Cleans up after job execution.
 */
public class CleanupWorker implements Worker {
    @Override public String getTaskDefName() { return "cj_cleanup"; }

    @Override public TaskResult execute(Task task) {
        String jobName = (String) task.getInputData().get("jobName");
        if (jobName == null) jobName = "unnamed";

        // Real cleanup: record completion
        System.out.println("  [cleanup] Cleaned up resources for " + jobName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleanedUp", true);
        result.getOutputData().put("cleanedAt", Instant.now().toString());
        return result;
    }
}
