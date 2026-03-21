package kycaml;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import kycaml.workers.VerifyIdentityWorker;
import kycaml.workers.ScreenWatchlistsWorker;
import kycaml.workers.AssessRiskWorker;
import kycaml.workers.DecideWorker;

import java.util.List;
import java.util.Map;

/**
 * KYC/AML Workflow Demo
 *
 * Demonstrates a sequential compliance pipeline:
 *   kyc_verify_identity -> kyc_screen_watchlists -> kyc_assess_risk -> kyc_decide
 *
 * Run:
 *   java -jar target/kyc-aml-1.0.0.jar
 */
public class KycAmlExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 493: KYC/AML ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "kyc_verify_identity", "kyc_screen_watchlists",
                "kyc_assess_risk", "kyc_decide"));
        System.out.println("  Registered: kyc_verify_identity, kyc_screen_watchlists, kyc_assess_risk, kyc_decide\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'kyc_aml_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new VerifyIdentityWorker(),
                new ScreenWatchlistsWorker(),
                new AssessRiskWorker(),
                new DecideWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("kyc_aml_workflow", 1,
                Map.of("customerId", "CUST-FIN-8801",
                       "name", "John Anderson",
                       "nationality", "US",
                       "documentType", "passport"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
