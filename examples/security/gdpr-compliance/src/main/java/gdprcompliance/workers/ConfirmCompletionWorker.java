package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirms GDPR request completion and generates compliance report.
 *
 * Input:
 *   - requestType (String, required): the GDPR request type
 *   - piiCount (int, required): number of PII items processed
 *   - subjectId (String, required): the data subject identifier
 *
 * Output:
 *   - completed (boolean): true
 *   - reportId (String): unique GDPR report identifier
 *   - piiItemsProcessed (int): number of PII items in the report
 *   - completedAt (String): ISO-8601 timestamp
 *   - auditLog (Map): timestamp, action, actor, result
 */
public class ConfirmCompletionWorker implements Worker {
    @Override public String getTaskDefName() { return "gdpr_confirm_completion"; }

    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // Validate required inputs
        String requestType = getRequiredString(task, "requestType");
        if (requestType == null || requestType.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: requestType");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_confirm_completion", "SYSTEM", "FAILED", "Missing requestType"));
            return result;
        }

        String subjectId = getRequiredString(task, "subjectId");
        if (subjectId == null || subjectId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: subjectId");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_confirm_completion", "SYSTEM", "FAILED", "Missing subjectId"));
            return result;
        }

        Object piiCountObj = task.getInputData().get("piiCount");
        if (piiCountObj == null) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: piiCount");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_confirm_completion", subjectId, "FAILED", "Missing piiCount"));
            return result;
        }

        int piiCount;
        if (piiCountObj instanceof Number) {
            piiCount = ((Number) piiCountObj).intValue();
        } else {
            try {
                piiCount = Integer.parseInt(piiCountObj.toString());
            } catch (NumberFormatException e) {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("Invalid piiCount: not a number: " + piiCountObj);
                result.getOutputData().put("auditLog",
                        VerifyIdentityWorker.auditLog("gdpr_confirm_completion", subjectId, "FAILED", "Invalid piiCount type"));
                return result;
            }
        }

        if (piiCount < 0) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid piiCount: must be >= 0, got " + piiCount);
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_confirm_completion", subjectId, "FAILED", "Negative piiCount"));
            return result;
        }

        String reportId = "GDPR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [gdpr_confirm] " + requestType + " request completed. Report: " + reportId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("completed", true);
        result.getOutputData().put("reportId", reportId);
        result.getOutputData().put("piiItemsProcessed", piiCount);
        result.getOutputData().put("completedAt", Instant.now().toString());
        result.getOutputData().put("auditLog",
                VerifyIdentityWorker.auditLog("gdpr_confirm_completion", subjectId, "SUCCESS",
                        requestType + " completed, " + piiCount + " PII items processed, report: " + reportId));
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
