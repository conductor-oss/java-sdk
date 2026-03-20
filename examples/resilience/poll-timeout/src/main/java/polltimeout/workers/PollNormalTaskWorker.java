package polltimeout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for the poll_normal_task task.
 * Picks up the task and processes it immediately.
 *
 * Takes a mode input and returns { result: "processed" }.
 * The pollTimeoutSeconds on the task definition controls how long
 * the task waits in the queue for a worker to pick it up.
 */
public class PollNormalTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "poll_normal_task";
    }

    @Override
    public TaskResult execute(Task task) {
        Object modeRaw = task.getInputData().get("mode");
        String mode;
        if (modeRaw == null) {
            mode = "default";
        } else {
            mode = String.valueOf(modeRaw);
            if (mode.isBlank()) {
                mode = "default";
            }
        }

        System.out.println("  [poll_normal_task] Processing with mode: " + mode);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed");
        return result;
    }
}
