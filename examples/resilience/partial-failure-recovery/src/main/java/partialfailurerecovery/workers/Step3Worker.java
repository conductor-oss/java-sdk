package partialfailurerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for pfr_step3 — third and final step in the partial failure recovery pipeline.
 *
 * Takes a "prev" input and returns { result: "s3-{prev}" }.
 */
public class Step3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfr_step3";
    }

    @Override
    public TaskResult execute(Task task) {
        String prev = "";
        Object prevInput = task.getInputData().get("prev");
        if (prevInput != null) {
            prev = prevInput.toString();
        }

        System.out.println("  [pfr_step3] Processing prev: " + prev);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "s3-" + prev);

        return result;
    }
}
