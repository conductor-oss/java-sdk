package notificationpreferences.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Loads current notification preferences.
 * Input: userId
 * Output: current
 */
public class LoadPrefsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "np_load";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");

        System.out.println("  [load] Loading current preferences for " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("current", Map.of(
                "email", true,
                "sms", false,
                "push", true,
                "slack", false
        ));
        return result;
    }
}
