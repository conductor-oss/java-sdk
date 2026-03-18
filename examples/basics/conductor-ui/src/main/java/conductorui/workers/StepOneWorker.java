package conductorui.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Step One — Processes user action input.
 *
 * Input:  userId, action
 * Output: result, timestamp
 */
public class StepOneWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ui_step_one";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String action = (String) task.getInputData().get("action");

        if (userId == null || userId.isBlank()) {
            userId = "unknown";
        }
        if (action == null || action.isBlank()) {
            action = "default";
        }

        String result = "Processed " + action + " for user " + userId;
        String timestamp = Instant.now().toString();

        System.out.println("  [ui_step_one] " + result);

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("result", result);
        taskResult.getOutputData().put("timestamp", timestamp);
        return taskResult;
    }
}
