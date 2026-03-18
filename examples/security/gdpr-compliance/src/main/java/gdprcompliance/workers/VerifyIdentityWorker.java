package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Verifies requestor identity for GDPR requests.
 */
public class VerifyIdentityWorker implements Worker {
    @Override public String getTaskDefName() { return "gdpr_verify_identity"; }

    @Override public TaskResult execute(Task task) {
        String requestorId = (String) task.getInputData().get("requestorId");
        String email = (String) task.getInputData().get("email");
        if (requestorId == null) requestorId = "UNKNOWN";
        if (email == null) email = "";

        boolean verified = !requestorId.equals("UNKNOWN") && email.contains("@");

        System.out.println("  [gdpr_verify] " + requestorId + ": verified=" + verified);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        return result;
    }
}
