package taskinputtemplates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches a request context with computed permissions based on user role.
 *
 * Input:  { requestContext: { user, action, metadata, environment, apiVersion, timestamp } }
 * Output: { enrichedContext: { ...requestContext, permissions: [...] } }
 */
public class BuildContextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tpl_build_context";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> ctx = (Map<String, Object>) task.getInputData().get("requestContext");
        Map<String, Object> user = (Map<String, Object>) ctx.get("user");
        String action = (String) ctx.get("action");

        System.out.println("  [context] " + user.get("name") + " -> " + action
                + " (" + ctx.get("environment") + ")");

        Map<String, Object> enriched = new HashMap<>(ctx);
        String role = (String) user.get("role");
        if ("admin".equals(role)) {
            enriched.put("permissions", List.of("read", "write", "delete"));
        } else {
            enriched.put("permissions", List.of("read"));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrichedContext", enriched);
        return result;
    }
}
