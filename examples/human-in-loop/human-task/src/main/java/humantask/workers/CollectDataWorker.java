package humantask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ht_collect_data — collects initial data before the human review step.
 *
 * Returns { collected: true } to indicate data has been gathered and is ready
 * for human review via the WAIT task (performing a HUMAN task with form schema).
 */
public class CollectDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ht_collect_data";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collected", true);
        return result;
    }
}
