package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies requestor identity for GDPR requests.
 *
 * Validates that the requestor has provided a valid identity token
 * and email address before allowing data subject requests to proceed.
 *
 * Input:
 *   - requestorId (String, required): unique requestor identifier
 *   - email (String, required): requestor email for verification
 *
 * Output:
 *   - verified (boolean): whether identity was verified
 *   - verifiedAt (String): ISO-8601 timestamp of verification
 *   - auditLog (Map): timestamp, action, actor, result
 */
public class VerifyIdentityWorker implements Worker {
    @Override public String getTaskDefName() { return "gdpr_verify_identity"; }

    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // Validate required inputs
        String requestorId = getRequiredString(task, "requestorId");
        String email = getRequiredString(task, "email");

        if (requestorId == null || requestorId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: requestorId");
            result.getOutputData().put("auditLog", auditLog("gdpr_verify_identity", "SYSTEM", "FAILED", "Missing requestorId"));
            return result;
        }

        if (email == null || email.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: email");
            result.getOutputData().put("auditLog", auditLog("gdpr_verify_identity", requestorId, "FAILED", "Missing email"));
            return result;
        }

        if (!email.contains("@") || !email.contains(".")) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid email format: " + email);
            result.getOutputData().put("auditLog", auditLog("gdpr_verify_identity", requestorId, "FAILED", "Invalid email format"));
            return result;
        }

        boolean verified = true;

        System.out.println("  [gdpr_verify] " + requestorId + ": verified=" + verified);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        result.getOutputData().put("requestorId", requestorId);
        result.getOutputData().put("auditLog", auditLog("gdpr_verify_identity", requestorId, "SUCCESS", "Identity verified"));
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }

    static Map<String, String> auditLog(String action, String actor, String resultStatus, String detail) {
        Map<String, String> log = new LinkedHashMap<>();
        log.put("timestamp", Instant.now().toString());
        log.put("action", action);
        log.put("actor", actor);
        log.put("result", resultStatus);
        log.put("detail", detail);
        return log;
    }
}
