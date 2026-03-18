package frauddetection;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import frauddetection.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Fraud Detection Demo
 *
 * Demonstrates a FORK_JOIN-based fraud detection workflow:
 *   frd_analyze_transaction -> FORK(frd_rule_check, frd_ml_score, frd_velocity_check) -> JOIN -> frd_decide
 *
 * Run:
 *   java -jar target/fraud-detection-1.0.0.jar
 */
public class FraudDetectionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Fraud Detection Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "frd_analyze_transaction", "frd_rule_check", "frd_ml_score",
                "frd_velocity_check", "frd_decide"));
        System.out.println("  Registered: frd_analyze_transaction, frd_rule_check, frd_ml_score, frd_velocity_check, frd_decide\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'fraud_detection_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalyzeTransactionWorker(),
                new RuleCheckWorker(),
                new MlScoreWorker(),
                new VelocityCheckWorker(),
                new DecideWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("fraud_detection_workflow", 1,
                Map.of("transactionId", "TXN-98321",
                        "amount", 249.99,
                        "merchantId", "MERCH-1234",
                        "customerId", "CUST-5678"));
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
