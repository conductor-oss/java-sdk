package datamigration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datamigration.workers.ExtractSourceWorker;
import datamigration.workers.ValidateSourceWorker;
import datamigration.workers.TransformSchemaWorker;
import datamigration.workers.LoadTargetWorker;
import datamigration.workers.VerifyMigrationWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Migration Workflow Demo
 *
 * Demonstrates a data migration pipeline:
 *   mi_extract_source -> mi_validate_source -> mi_transform_schema -> mi_load_target -> mi_verify_migration
 *
 * Run:
 *   java -jar target/data-migration-1.0.0.jar
 */
public class DataMigrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Migration Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mi_extract_source", "mi_validate_source", "mi_transform_schema",
                "mi_load_target", "mi_verify_migration"));
        System.out.println("  Registered: mi_extract_source, mi_validate_source, mi_transform_schema, mi_load_target, mi_verify_migration\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_migration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExtractSourceWorker(),
                new ValidateSourceWorker(),
                new TransformSchemaWorker(),
                new LoadTargetWorker(),
                new VerifyMigrationWorker()
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
        String workflowId = client.startWorkflow("data_migration", 1,
                Map.of("sourceConfig", Map.of("database", "legacy_hr_db", "table", "employees"),
                        "targetConfig", Map.of("database", "new_hr_system", "table", "employees_v2"),
                        "batchSize", 1000));
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
