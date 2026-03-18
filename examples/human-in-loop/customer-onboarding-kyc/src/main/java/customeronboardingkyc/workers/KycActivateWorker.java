package customeronboardingkyc.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for kyc_activate task -- activates the customer account after KYC approval.
 *
 * Returns activated=true to signal that onboarding is complete.
 */
public class KycActivateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kyc_activate";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = getStringInput(task, "customerId", "unknown");
        String customerName = getStringInput(task, "customerName", "unknown");

        System.out.println("  [kyc_activate] Activating customer: " + customerName
                + " (id=" + customerId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("activated", true);
        result.getOutputData().put("customerId", customerId);
        result.getOutputData().put("customerName", customerName);

        System.out.println("  [kyc_activate] Customer activated successfully.");
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
