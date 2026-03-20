package workflowtimeout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Fast worker that completes immediately.
 * Returns { result: "done-{mode}" } based on the input mode.
 */
public class FastWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wft_fast";
    }

    @Override
    public TaskResult execute(Task task) {
        String mode = (String) task.getInputData().get("mode");
        if (mode == null || mode.isBlank()) {
            mode = "default";
        }

        System.out.println("  [wft_fast] Processing with mode: " + mode);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "done-" + mode);
        return result;
    }
}
