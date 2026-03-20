package canaryrelease;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import canaryrelease.workers.*;
import java.util.List;
import java.util.Map;

public class CanaryReleaseExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 297: Canary Release ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cy_deploy_canary", "cy_monitor_canary", "cy_increase_traffic", "cy_full_rollout"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new DeployCanaryWorker(), new MonitorCanaryWorker(), new IncreaseTrafficWorker(), new FullRolloutWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("canary_release_297", 1,
                Map.of("appName", "user-service", "newVersion", "3.1.0"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
