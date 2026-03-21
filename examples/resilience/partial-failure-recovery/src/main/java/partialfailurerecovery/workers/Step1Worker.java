package partialfailurerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for pfr_step1 — first step in the partial failure recovery pipeline.
 *
 * Takes a "data" input and returns { result: "s1-{data}" }.
 */
public class Step1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfr_step1";
    }

    @Override
    public TaskResult execute(Task task) {
        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput != null) {
            data = dataInput.toString();
        }

        System.out.println("  [pfr_step1] Processing data: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "s1-" + data);

        return result;
    }
}
