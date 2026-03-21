package taskinputtemplates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Executes an action, checking permissions from the enriched context.
 *
 * Input:  { userName, userRole, action, context: { permissions, ... } }
 * Output: { result: "success"|"permission_denied", auditLog: { user, action, allowed, timestamp } }
 */
public class ExecuteActionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tpl_execute_action";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String userName = (String) task.getInputData().get("userName");
        String userRole = (String) task.getInputData().get("userRole");
        String action = (String) task.getInputData().get("action");
        Map<String, Object> context = (Map<String, Object>) task.getInputData().get("context");

        List<String> permissions = (List<String>) context.get("permissions");
        boolean allowed = permissions.contains("write") || "read".equals(action);

        System.out.println("  [execute] " + userName + " (" + userRole + ") -> " + action
                + ": " + (allowed ? "ALLOWED" : "DENIED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", allowed ? "success" : "permission_denied");
        result.getOutputData().put("auditLog", Map.of(
                "user", userName,
                "action", action,
                "allowed", allowed,
                "timestamp", Instant.now().toString()
        ));
        return result;
    }
}
