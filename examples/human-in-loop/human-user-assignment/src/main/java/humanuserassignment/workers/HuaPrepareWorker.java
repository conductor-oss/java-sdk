package humanuserassignment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for hua_prepare — prepares a document for human review.
 *
 * Returns { prepared: true } on success.
 */
public class HuaPrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hua_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [hua_prepare] Preparing document for review...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prepared", true);

        return result;
    }
}
