package gracefuldegradation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for gd_core_process -- the required core processing step.
 *
 * Returns { result: "processed-{data}" } where data comes from the input.
 */
public class CoreProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_core_process";
    }

    @Override
    public TaskResult execute(Task task) {
        Object dataInput = task.getInputData().get("data");
        String data = dataInput != null ? dataInput.toString() : "default";

        System.out.println("  [gd_core_process] Processing data: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed-" + data);
        return result;
    }
}
