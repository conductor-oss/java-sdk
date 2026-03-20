package centralizedconfigmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import centralizedconfigmanagement.workers.*;
import java.util.List;
import java.util.Map;

public class CentralizedConfigManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 318: Config Management (Centralized Rollout) ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cfg_validate", "cfg_stage_rollout", "cfg_apply_config", "cfg_verify"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CfgValidateWorker(), new CfgStageRolloutWorker(), new CfgApplyWorker(), new CfgVerifyWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("config_management_workflow", 1,
                Map.of("configKey", "max_connections", "configValue", "200", "targetServices", List.of("api", "worker", "scheduler")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
