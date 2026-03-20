package networkautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plans network configuration changes based on the audit.
 * Input: plan_changesData (from audit output)
 * Output: firewallRules, vlanChanges, planned, processed
 */
public class PlanChanges implements Worker {

    @Override
    public String getTaskDefName() {
        return "na_plan_changes";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("plan_changesData");
        String changeType = "general";
        if (data != null && data.get("changeType") != null) {
            changeType = (String) data.get("changeType");
        }

        int firewallRules;
        int vlanChanges;
        switch (changeType) {
            case "firewall-update":
                firewallRules = 3;
                vlanChanges = 2;
                break;
            case "vlan-migration":
                firewallRules = 1;
                vlanChanges = 5;
                break;
            default:
                firewallRules = 2;
                vlanChanges = 1;
                break;
        }

        System.out.println("[na_plan_changes] " + firewallRules + " firewall rules, " + vlanChanges + " VLAN changes planned");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("firewallRules", firewallRules);
        output.put("vlanChanges", vlanChanges);
        output.put("planned", true);
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
