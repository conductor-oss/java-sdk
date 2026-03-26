package datareconciliation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datareconciliation.workers.FetchSourceAWorker;
import datareconciliation.workers.FetchSourceBWorker;
import datareconciliation.workers.CompareRecordsWorker;
import datareconciliation.workers.GenerateDiscrepancyReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Reconciliation Workflow Demo
 *
 * Demonstrates a data reconciliation pipeline:
 *   rc_fetch_source_a -> rc_fetch_source_b -> rc_compare_records -> rc_generate_discrepancy_report
 *
 * Run:
 *   java -jar target/data-reconciliation-1.0.0.jar
 */
public class DataReconciliationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Reconciliation Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rc_fetch_source_a", "rc_fetch_source_b",
                "rc_compare_records", "rc_generate_discrepancy_report"));
        System.out.println("  Registered: rc_fetch_source_a, rc_fetch_source_b, rc_compare_records, rc_generate_discrepancy_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_reconciliation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new FetchSourceAWorker(),
                new FetchSourceBWorker(),
                new CompareRecordsWorker(),
                new GenerateDiscrepancyReportWorker()
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
        String workflowId = client.startWorkflow("data_reconciliation", 1,
                Map.of("sourceA", Map.of("system", "billing", "table", "orders"),
                        "sourceB", Map.of("system", "fulfillment", "table", "shipments"),
                        "keyField", "orderId"));
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
