package datavalidation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datavalidation.workers.LoadRecordsWorker;
import datavalidation.workers.CheckRequiredWorker;
import datavalidation.workers.CheckTypesWorker;
import datavalidation.workers.CheckRangesWorker;
import datavalidation.workers.GenerateReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Validation Demo
 *
 * Sequential validation pipeline:
 *   vd_load_records -> vd_check_required -> vd_check_types -> vd_check_ranges -> vd_generate_report
 *
 * Run:
 *   java -jar target/data-validation-1.0.0.jar
 */
public class DataValidationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Validation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "vd_load_records", "vd_check_required",
                "vd_check_types", "vd_check_ranges", "vd_generate_report"));
        System.out.println("  Registered: vd_load_records, vd_check_required, vd_check_types, vd_check_ranges, vd_generate_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_validation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadRecordsWorker(),
                new CheckRequiredWorker(),
                new CheckTypesWorker(),
                new CheckRangesWorker(),
                new GenerateReportWorker()
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
        String workflowId = client.startWorkflow("data_validation", 1,
                Map.of("records", List.of(
                                Map.of("name", "Alice", "email", "alice@example.com", "age", 30),
                                Map.of("name", "Bob", "email", "bob@example.com", "age", 25),
                                Map.of("name", "", "email", "noname@test.com", "age", 40),
                                Map.of("name", "Charlie", "email", "charlie@example.com", "age", -5),
                                Map.of("name", "Diana", "email", "diana@example.com", "age", 35)),
                        "schema", Map.of(
                                "requiredFields", List.of("name", "email", "age"),
                                "types", Map.of("name", "string", "age", "number"))));
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
