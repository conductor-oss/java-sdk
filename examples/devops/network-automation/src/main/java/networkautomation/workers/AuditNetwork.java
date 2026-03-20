package networkautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audits network infrastructure to discover devices and current configuration.
 * Input: network, changeType
 * Output: auditId, network, changeType, switches, routers, success
 */
public class AuditNetwork implements Worker {

    @Override
    public String getTaskDefName() {
        return "na_audit";
    }

    @Override
    public TaskResult execute(Task task) {
        String network = (String) task.getInputData().get("network");
        if (network == null) network = "default-network";

        String changeType = (String) task.getInputData().get("changeType");
        if (changeType == null) changeType = "general";

        int switches;
        int routers;
        switch (network) {
            case "prod-datacenter":
                switches = 8;
                routers = 4;
                break;
            case "staging-datacenter":
                switches = 4;
                routers = 2;
                break;
            default:
                switches = 2;
                routers = 1;
                break;
        }

        System.out.println("[na_audit] Network " + network + ": " + switches + " switches, " + routers + " routers");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("auditId", "AUDIT-1353");
        output.put("network", network);
        output.put("changeType", changeType);
        output.put("switches", switches);
        output.put("routers", routers);
        output.put("totalDevices", switches + routers);
        output.put("success", true);
        result.setOutputData(output);
        return result;
    }
}
