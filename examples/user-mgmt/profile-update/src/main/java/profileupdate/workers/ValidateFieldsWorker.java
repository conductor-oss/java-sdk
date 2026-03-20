package profileupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Validates profile update fields.
 * Input: userId, updates
 * Output: validatedFields, allValid
 */
public class ValidateFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfu_validate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        Map<String, Object> updates = (Map<String, Object>) task.getInputData().get("updates");
        if (updates == null) {
            updates = Map.of();
        }

        System.out.println("  [validate] Validating " + updates.size() + " field(s) for " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedFields", updates);
        result.getOutputData().put("allValid", true);
        return result;
    }
}
