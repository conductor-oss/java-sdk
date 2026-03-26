package notificationpreferences.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Merges and updates notification preferences.
 * Input: userId, current, newPrefs
 * Output: updated
 */
public class UpdatePrefsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "np_update";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> current = (Map<String, Object>) task.getInputData().get("current");
        Map<String, Object> newPrefs = (Map<String, Object>) task.getInputData().get("newPrefs");

        Map<String, Object> merged = new HashMap<>();
        if (current != null) merged.putAll(current);
        if (newPrefs != null) merged.putAll(newPrefs);

        System.out.println("  [update] Preferences updated: " + merged);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updated", merged);
        return result;
    }
}
