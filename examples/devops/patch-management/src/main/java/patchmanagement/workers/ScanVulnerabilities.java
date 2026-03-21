package patchmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans systems for vulnerabilities matching a given patch.
 * Input: patchId, severity
 * Output: scanId, patchId, severity, affectedHosts, vulnerabilityCount, success
 */
public class ScanVulnerabilities implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_scan_vulnerabilities";
    }

    @Override
    public TaskResult execute(Task task) {
        String patchId = (String) task.getInputData().get("patchId");
        if (patchId == null) patchId = "PATCH-UNKNOWN";

        String severity = (String) task.getInputData().get("severity");
        if (severity == null) severity = "medium";

        int affectedHosts;
        switch (severity) {
            case "critical": affectedHosts = 24; break;
            case "high": affectedHosts = 16; break;
            case "low": affectedHosts = 4; break;
            default: affectedHosts = 8; break;
        }

        System.out.println("[pm_scan_vulnerabilities] Patch " + patchId + ": " + affectedHosts + " affected hosts found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("scanId", "SCAN-1352");
        output.put("patchId", patchId);
        output.put("severity", severity);
        output.put("affectedHosts", affectedHosts);
        output.put("vulnerabilityCount", affectedHosts * 2);
        output.put("success", true);
        result.setOutputData(output);
        return result;
    }
}
