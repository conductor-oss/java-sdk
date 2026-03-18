package bluegreendeploy;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bluegreendeploy.workers.*;
import java.util.List;
import java.util.Map;

public class BlueGreenDeployExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 296: Blue-Green Deployment ===\n");

        var client = new ConductorClientHelper();
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("bg_prepare_green", "bg_test_green", "bg_switch_traffic", "bg_verify_deployment"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PrepareGreenWorker(), new TestGreenWorker(), new SwitchTrafficWorker(), new VerifyDeploymentWorker());
        client.startWorkers(workers);

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...");
        String workflowId = client.startWorkflow("blue_green_deploy_296", 1,
                Map.of("appName", "payment-service", "newVersion", "2.5.0", "currentEnv", "blue"));
        System.out.println("  Workflow ID: " + workflowId);

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
