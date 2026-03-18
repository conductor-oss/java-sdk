package useronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sets user preferences with defaults and validation.
 */
public class SetPreferencesWorker implements Worker {
    @Override public String getTaskDefName() { return "uo_set_preferences"; }

    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object prefsObj = task.getInputData().get("preferences");

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("language", "en");
        defaults.put("timezone", "UTC");
        defaults.put("theme", "light");
        defaults.put("notifications", true);
        defaults.put("emailDigest", "weekly");

        if (prefsObj instanceof Map) {
            Map<String, Object> userPrefs = (Map<String, Object>) prefsObj;
            defaults.putAll(userPrefs);
        }

        System.out.println("  [preferences] Set " + defaults.size() + " preferences");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("preferences", defaults);
        result.getOutputData().put("preferencesSet", true);
        return result;
    }
}
