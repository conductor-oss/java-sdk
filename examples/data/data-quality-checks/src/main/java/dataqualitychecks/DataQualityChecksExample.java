package dataqualitychecks;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataqualitychecks.workers.LoadDataWorker;
import dataqualitychecks.workers.CheckCompletenessWorker;
import dataqualitychecks.workers.CheckAccuracyWorker;
import dataqualitychecks.workers.CheckConsistencyWorker;
import dataqualitychecks.workers.GenerateReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Quality Checks Workflow Demo
 *
 * Demonstrates parallel quality checks using FORK_JOIN:
 *   qc_load_data -> FORK(qc_check_completeness, qc_check_accuracy, qc_check_consistency) -> JOIN -> qc_generate_report
 *
 * Run:
 *   java -jar target/data-quality-checks-1.0.0.jar
 */
public class DataQualityChecksExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Quality Checks Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "qc_load_data", "qc_check_completeness", "qc_check_accuracy",
                "qc_check_consistency", "qc_generate_report"));
        System.out.println("  Registered: qc_load_data, qc_check_completeness, qc_check_accuracy, qc_check_consistency, qc_generate_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_quality_checks'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadDataWorker(),
                new CheckCompletenessWorker(),
                new CheckAccuracyWorker(),
                new CheckConsistencyWorker(),
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
        String workflowId = client.startWorkflow("data_quality_checks", 1,
                Map.of("records", List.of(
                        Map.of("id", 1, "name", "Alice", "email", "alice@example.com", "status", "active"),
                        Map.of("id", 2, "name", "Bob", "email", "bob@example.com", "status", "active"),
                        Map.of("id", 3, "name", "", "email", "charlie@example.com", "status", "inactive"),
                        Map.of("id", 4, "name", "Diana", "email", "diana@example.com", "status", "pending"),
                        Map.of("id", 5, "name", "Eve", "email", "eve@example.com", "status", "active"),
                        Map.of("id", 6, "name", "Frank", "email", "invalid-email", "status", "active")
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
