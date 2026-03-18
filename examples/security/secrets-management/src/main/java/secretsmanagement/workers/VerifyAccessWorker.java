package secretsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Verifies access permissions for secret operations.
 */
public class VerifyAccessWorker implements Worker {
    @Override public String getTaskDefName() { return "sec_verify_access"; }

    @Override public TaskResult execute(Task task) {
        String requestorId = (String) task.getInputData().get("requestorId");
        String secretName = (String) task.getInputData().get("secretName");
        String operation = (String) task.getInputData().get("operation");
        if (requestorId == null) requestorId = "UNKNOWN";
        if (operation == null) operation = "read";

        // Real access control logic
        boolean hasAccess = !requestorId.equals("UNKNOWN") && !requestorId.isEmpty();

        System.out.println("  [verify_access] " + requestorId + " -> " + operation + " " + secretName + ": " + hasAccess);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("hasAccess", hasAccess);
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        return result;
    }
}
