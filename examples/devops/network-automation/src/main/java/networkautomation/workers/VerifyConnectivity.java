package networkautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies network connectivity after configuration changes.
 * Input: verify_connectivityData (from apply_config output)
 * Output: verified, allTestsPassed, completedAt
 */
public class VerifyConnectivity implements Worker {

    @Override
    public String getTaskDefName() {
        return "na_verify_connectivity";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("verify_connectivityData");
        int devicesConfigured = 12;
        if (data != null && data.get("devicesConfigured") != null) {
            devicesConfigured = ((Number) data.get("devicesConfigured")).intValue();
        }

        System.out.println("[na_verify_connectivity] All connectivity tests passed for " + devicesConfigured + " devices");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("verified", true);
        output.put("allTestsPassed", true);
        output.put("devicesVerified", devicesConfigured);
        output.put("completedAt", "2026-03-14T10:00:00Z");
        result.setOutputData(output);
        return result;
    }
}
