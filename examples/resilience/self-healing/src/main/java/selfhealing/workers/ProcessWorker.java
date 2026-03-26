package selfhealing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sh_process — normal processing for healthy services.
 *
 * Returns result="processed-{data}" where data comes from the input.
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput != null) {
            data = dataInput.toString();
        }

        System.out.println("  [sh_process] Processing data: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed-" + data);

        return result;
    }
}
