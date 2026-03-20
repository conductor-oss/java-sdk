package paymentreconciliation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import paymentreconciliation.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Payment Reconciliation Demo
 *
 * Demonstrates a sequential payment reconciliation workflow:
 *   prc_match_transactions -> prc_identify_discrepancies -> prc_resolve_mismatches -> prc_generate_report
 *
 * Run:
 *   java -jar target/payment-reconciliation-1.0.0.jar
 */
public class PaymentReconciliationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Payment Reconciliation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "prc_match_transactions", "prc_identify_discrepancies",
                "prc_resolve_mismatches", "prc_generate_report"));
        System.out.println("  Registered: prc_match_transactions, prc_identify_discrepancies, prc_resolve_mismatches, prc_generate_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'payment_reconciliation_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MatchTransactionsWorker(),
                new IdentifyDiscrepanciesWorker(),
                new ResolveMismatchesWorker(),
                new GenerateReportWorker()
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
        String workflowId = client.startWorkflow("payment_reconciliation_workflow", 1,
                Map.of("batchId", "BATCH-2026-0314",
                        "accountId", "ACCT-7890",
                        "periodStart", "2026-03-01",
                        "periodEnd", "2026-03-14"));
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
