package switchplusfork.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Single item processor for the default (non-batch) case.
 * Returns mode: "single" to indicate single-item processing was used.
 */
public class SingleProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sf_single_process";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sf_single_process] Processing single item");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mode", "single");
        return result;
    }
}
