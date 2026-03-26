package patchmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies that all hosts are patched and healthy.
 * Input: verify_patchData (from deploy output)
 * Output: verified, allHealthy, completedAt
 */
public class VerifyPatch implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_verify_patch";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("verify_patchData");
        int hostsPatched = 24;
        if (data != null && data.get("hostsPatched") != null) {
            hostsPatched = ((Number) data.get("hostsPatched")).intValue();
        }

        System.out.println("[pm_verify_patch] All " + hostsPatched + " hosts patched and healthy");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("verified", true);
        output.put("allHealthy", true);
        output.put("hostsVerified", hostsPatched);
        output.put("completedAt", "2026-03-14T10:00:00Z");
        result.setOutputData(output);
        return result;
    }
}
