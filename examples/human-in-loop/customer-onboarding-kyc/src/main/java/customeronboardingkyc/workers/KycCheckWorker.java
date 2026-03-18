package customeronboardingkyc.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Worker for kyc_check task -- performs automated KYC risk assessment.
 *
 * If riskLevel is "high", sets needsReview="true" and includes flags
 * for manual review. Otherwise, sets needsReview="false" for auto-approval.
 */
public class KycCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kyc_check";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = getStringInput(task, "customerId", "unknown");
        String customerName = getStringInput(task, "customerName", "unknown");
        String riskLevel = getStringInput(task, "riskLevel", "low");

        System.out.println("  [kyc_check] Checking customer: " + customerName
                + " (id=" + customerId + ", risk=" + riskLevel + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("customerId", customerId);
        result.getOutputData().put("customerName", customerName);
        result.getOutputData().put("riskLevel", riskLevel);

        if ("high".equalsIgnoreCase(riskLevel)) {
            result.getOutputData().put("needsReview", "true");
            result.getOutputData().put("flags", List.of("HIGH_RISK", "MANUAL_REVIEW_REQUIRED"));
            System.out.println("  [kyc_check] High risk -- flagged for manual review.");
        } else {
            result.getOutputData().put("needsReview", "false");
            result.getOutputData().put("flags", List.of());
            System.out.println("  [kyc_check] Low risk -- auto-approved.");
        }
        return result;
    }

    private String getStringInput(Task task, String key, String defaultValue) {
        Object value = task.getInputData().get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}
