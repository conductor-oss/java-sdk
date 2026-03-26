package bulkoperations.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Second step in the bulk operations workflow.
 * Takes intermediate data from Step1 and produces the final result.
 */
public class Step2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bulk_step2";
    }

    @Override
    public TaskResult execute(Task task) {
        String data = (String) task.getInputData().get("data");
        if (data == null || data.isBlank()) {
            data = "unknown";
        }

        System.out.println("  [bulk_step2] Finalizing: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "done-" + data);
        return result;
    }
}
