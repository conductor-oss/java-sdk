package csvprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import csvprocessing.workers.ParseCsvWorker;
import csvprocessing.workers.ValidateRowsWorker;
import csvprocessing.workers.TransformFieldsWorker;
import csvprocessing.workers.GenerateOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * CSV Processing Demo
 *
 * Demonstrates a sequential data-processing workflow:
 *   cv_parse_csv -> cv_validate_rows -> cv_transform_fields -> cv_generate_output
 *
 * Run:
 *   java -jar target/csv-processing-1.0.0.jar
 */
public class CsvProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== CSV Processing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cv_parse_csv", "cv_validate_rows",
                "cv_transform_fields", "cv_generate_output"));
        System.out.println("  Registered: cv_parse_csv, cv_validate_rows, cv_transform_fields, cv_generate_output\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'csv_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseCsvWorker(),
                new ValidateRowsWorker(),
                new TransformFieldsWorker(),
                new GenerateOutputWorker()
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
        String csvData = "name,email,dept,salary\n"
                + "Alice,alice@corp.com,engineering,95000\n"
                + "Bob,bob@corp.com,sales,82000\n"
                + ",invalid-email,hr,70000\n"
                + "Diana,diana@corp.com,engineering,105000";
        String workflowId = client.startWorkflow("csv_processing", 1,
                Map.of("csvData", csvData,
                        "delimiter", ",",
                        "hasHeader", true));
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
