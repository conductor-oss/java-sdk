package taskinputtemplates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Map;

/**
 * Looks up a user by ID and returns their profile.
 *
 * Input:  { userId: "U-1" }
 * Output: { user: { name, role, department }, timestamp: ISO string }
 */
public class LookupUserWorker implements Worker {

    private static final Map<String, Map<String, String>> USERS = Map.of(
            "U-1", Map.of("name", "Alice", "role", "admin", "department", "Engineering"),
            "U-2", Map.of("name", "Bob", "role", "viewer", "department", "Marketing")
    );

    @Override
    public String getTaskDefName() {
        return "tpl_lookup_user";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        Map<String, String> user = USERS.getOrDefault(userId,
                Map.of("name", "Unknown", "role", "guest"));

        System.out.println("  [lookup] User: " + user.get("name") + " (" + user.get("role") + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("user", user);
        result.getOutputData().put("timestamp", Instant.now().toString());
        return result;
    }
}
