package datawarehouseload;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datawarehouseload.workers.StageDataWorker;
import datawarehouseload.workers.PreLoadChecksWorker;
import datawarehouseload.workers.UpsertTargetWorker;
import datawarehouseload.workers.PostLoadValidationWorker;
import datawarehouseload.workers.UpdateMetadataWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Warehouse Load Workflow Demo
 *
 * Demonstrates a warehouse load pipeline:
 *   wh_stage_data -> wh_pre_load_checks -> wh_upsert_target -> wh_post_load_validation -> wh_update_metadata
 *
 * Run:
 *   java -jar target/data-warehouse-load-1.0.0.jar
 */
public class DataWarehouseLoadExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Warehouse Load Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wh_stage_data", "wh_pre_load_checks", "wh_upsert_target",
                "wh_post_load_validation", "wh_update_metadata"));
        System.out.println("  Registered: wh_stage_data, wh_pre_load_checks, wh_upsert_target, wh_post_load_validation, wh_update_metadata\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_warehouse_load'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new StageDataWorker(),
                new PreLoadChecksWorker(),
                new UpsertTargetWorker(),
                new PostLoadValidationWorker(),
                new UpdateMetadataWorker()
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
        String workflowId = client.startWorkflow("data_warehouse_load", 1,
                Map.of("records", List.of(
                        Map.of("customer_id", "C-001", "revenue", 15000, "quarter", "Q1-2025"),
                        Map.of("customer_id", "C-002", "revenue", 22000, "quarter", "Q1-2025"),
                        Map.of("customer_id", "C-003", "revenue", 8500, "quarter", "Q1-2025"),
                        Map.of("customer_id", "C-001", "revenue", 18000, "quarter", "Q2-2025"),
                        Map.of("customer_id", "C-004", "revenue", 31000, "quarter", "Q1-2025")
                ),
                "targetTable", "fact_customer_revenue",
                "schema", "analytics"));
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
