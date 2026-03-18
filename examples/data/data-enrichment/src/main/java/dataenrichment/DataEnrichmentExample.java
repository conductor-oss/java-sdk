package dataenrichment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataenrichment.workers.LoadRecordsWorker;
import dataenrichment.workers.LookupGeoWorker;
import dataenrichment.workers.LookupCompanyWorker;
import dataenrichment.workers.LookupCreditWorker;
import dataenrichment.workers.MergeEnrichedWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Enrichment Workflow Demo
 *
 * Demonstrates a sequential enrichment pipeline:
 *   dr_load_records -> dr_lookup_geo -> dr_lookup_company -> dr_lookup_credit -> dr_merge_enriched
 *
 * Run:
 *   java -jar target/data-enrichment-1.0.0.jar
 */
public class DataEnrichmentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Enrichment Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dr_load_records", "dr_lookup_geo", "dr_lookup_company",
                "dr_lookup_credit", "dr_merge_enriched"));
        System.out.println("  Registered: dr_load_records, dr_lookup_geo, dr_lookup_company, dr_lookup_credit, dr_merge_enriched\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_enrichment'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadRecordsWorker(),
                new LookupGeoWorker(),
                new LookupCompanyWorker(),
                new LookupCreditWorker(),
                new MergeEnrichedWorker()
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
        String workflowId = client.startWorkflow("data_enrichment", 1,
                Map.of("records", List.of(
                        Map.of("id", 1, "name", "Alice", "email", "alice@acme.com", "zip", "94105"),
                        Map.of("id", 2, "name", "Bob", "email", "bob@globex.com", "zip", "10001"),
                        Map.of("id", 3, "name", "Charlie", "email", "charlie@initech.com", "zip", "60601")
                )));
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
