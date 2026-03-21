package multiregiondeploy;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multiregiondeploy.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Multi-Region Deploy Demo -- Coordinated Cross-Region Deployment
 *
 * Pattern:
 *   deploy-primary -> verify-primary -> deploy-secondary -> verify-global
 */
public class MultiRegionDeployExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Region Deploy Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mrd_deploy_primary", "mrd_verify_primary", "mrd_deploy_secondary", "mrd_verify_global"));
        System.out.println("  Registered.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DeployPrimaryWorker(), new VerifyPrimaryWorker(),
                new DeploySecondaryWorker(), new VerifyGlobalWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("multi_region_deploy_workflow", 1,
                Map.of("service", "api-gateway", "version", "3.0.0",
                        "regions", List.of("us-east-1", "eu-west-1", "ap-southeast-1")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
