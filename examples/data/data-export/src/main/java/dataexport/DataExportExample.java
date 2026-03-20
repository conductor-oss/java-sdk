package dataexport;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataexport.workers.PrepareDataWorker;
import dataexport.workers.ExportCsvWorker;
import dataexport.workers.ExportJsonWorker;
import dataexport.workers.ExportExcelWorker;
import dataexport.workers.BundleExportsWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Export Workflow Demo
 *
 * Demonstrates a parallel export pipeline using FORK_JOIN:
 *   dx_prepare_data -> FORK(dx_export_csv, dx_export_json, dx_export_excel) -> JOIN -> dx_bundle_exports
 *
 * Run:
 *   java -jar target/data-export-1.0.0.jar
 */
public class DataExportExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Export Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dx_prepare_data", "dx_export_csv", "dx_export_json",
                "dx_export_excel", "dx_bundle_exports"));
        System.out.println("  Registered: dx_prepare_data, dx_export_csv, dx_export_json, dx_export_excel, dx_bundle_exports\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'data_export'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareDataWorker(),
                new ExportCsvWorker(),
                new ExportJsonWorker(),
                new ExportExcelWorker(),
                new BundleExportsWorker()
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

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_export", 1,
                Map.of("query", Map.of("table", "products", "filters", Map.of("active", true)),
                        "formats", List.of("csv", "json", "excel"),
                        "destination", "s3://data-exports/2024-03"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
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
