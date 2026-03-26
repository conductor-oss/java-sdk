package optionaltasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for opt_required — processes the input data.
 *
 * Returns { result: "processed-{data}" } on success.
 */
public class RequiredWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opt_required";
    }

    @Override
    public TaskResult execute(Task task) {
        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput != null) {
            data = dataInput.toString();
        }

        System.out.println("  [opt_required] Processing data: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed-" + data);
        return result;
    }
}
