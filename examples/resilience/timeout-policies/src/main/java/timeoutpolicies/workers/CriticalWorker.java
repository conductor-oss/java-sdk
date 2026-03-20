package timeoutpolicies.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for the tp_critical task.
 *
 * The task definition uses timeoutPolicy: TIME_OUT_WF.
 * If this worker does not respond within responseTimeoutSeconds,
 * the entire workflow is terminated.
 */
public class CriticalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tp_critical";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tp_critical] Processing -- responds within timeout");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "critical-done");
        return result;
    }
}
