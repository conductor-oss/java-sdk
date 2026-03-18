package claimsprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import claimsprocessing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Claims Processing Workflow Demo
 *
 * Insurance claims workflow: submit claim, verify details, assess damage,
 * settle amount, and close claim.
 *
 * Run:
 *   java -jar target/claims-processing-1.0.0.jar
 */
public class ClaimsProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 501: Claims Processing ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "clp_submit_claim", "clp_verify_details", "clp_assess_damage",
                "clp_settle_amount", "clp_close_claim"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SubmitClaimWorker(),
                new VerifyDetailsWorker(),
                new AssessDamageWorker(),
                new SettleAmountWorker(),
                new CloseClaimWorker()
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
        String workflowId = client.startWorkflow("claims_processing_workflow", 1,
                Map.of("claimId", "CLM-8810",
                        "policyId", "POL-3322",
                        "claimType", "auto_collision",
                        "description", "Rear-end collision at intersection",
                        "amount", 12000));
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
