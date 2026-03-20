package vendoronboarding;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import vendoronboarding.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 653: Vendor Onboarding — New Supplier Registration
 *
 * Demonstrates: apply -> verify -> evaluate -> approve -> activate
 *
 * Run:
 *   java -jar target/vendor-onboarding-1.0.0.jar
 */
public class VendorOnboardingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 653: Vendor Onboarding ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "von_apply", "von_verify", "von_evaluate", "von_approve", "von_activate"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'von_vendor_onboarding'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ApplyWorker(),
                new VerifyWorker(),
                new EvaluateWorker(),
                new ApproveWorker(),
                new ActivateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("von_vendor_onboarding", 1,
                Map.of("vendorName", "TechParts Global",
                       "category", "electronics",
                       "country", "Germany"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
