package timeoutpolicies.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for the tp_optional task.
 *
 * The task definition uses timeoutPolicy: ALERT_ONLY.
 * If this worker does not respond within responseTimeoutSeconds,
 * the task is marked TIMED_OUT but the workflow continues.
 */
public class OptionalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tp_optional";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tp_optional] Processing -- optional enrichment step");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "optional-done");
        return result;
    }
}
