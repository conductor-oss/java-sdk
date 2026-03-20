package taskdefinitions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Fast task worker for the task_def_test workflow.
 * Simply returns { done: true } to confirm the task definition is working.
 *
 * The task definition for td_fast_task is configured with:
 * - retryCount: 1 (FIXED, 1s delay)
 * - timeoutSeconds: 10
 * - responseTimeoutSeconds: 5
 */
public class FastTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "td_fast_task";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [td_fast_task] Executing fast task");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);
        return result;
    }
}
