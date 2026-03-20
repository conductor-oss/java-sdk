package profileupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Applies the profile updates.
 * Input: userId, validatedFields
 * Output: updatedFields, updatedAt
 */
public class UpdateProfileWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfu_update";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> validatedFields = (Map<String, Object>) task.getInputData().get("validatedFields");
        if (validatedFields == null) {
            validatedFields = Map.of();
        }
        List<String> fields = List.copyOf(validatedFields.keySet());

        System.out.println("  [update] Updated fields: " + String.join(", ", fields));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updatedFields", fields);
        result.getOutputData().put("updatedAt", Instant.now().toString());
        return result;
    }
}
