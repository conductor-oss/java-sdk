package schemaevolution;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import schemaevolution.workers.DetectChangesWorker;
import schemaevolution.workers.GenerateTransformWorker;
import schemaevolution.workers.ApplyTransformWorker;
import schemaevolution.workers.ValidateSchemaWorker;

import java.util.List;
import java.util.Map;

/**
 * Schema Evolution Workflow Demo
 *
 * Demonstrates schema evolution handling:
 *   sh_detect_changes -> sh_generate_transform -> sh_apply_transform -> sh_validate_schema
 *
 * Run:
 *   java -jar target/schema-evolution-1.0.0.jar
 */
public class SchemaEvolutionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Schema Evolution Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sh_detect_changes", "sh_generate_transform",
                "sh_apply_transform", "sh_validate_schema"));
        System.out.println("  Registered: sh_detect_changes, sh_generate_transform, sh_apply_transform, sh_validate_schema\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'schema_evolution'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectChangesWorker(),
                new GenerateTransformWorker(),
                new ApplyTransformWorker(),
                new ValidateSchemaWorker()
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
        String workflowId = client.startWorkflow("schema_evolution", 1,
                Map.of("currentSchema", Map.of("version", "v1", "fields", List.of("id", "name", "phone", "age", "legacy_id")),
                        "targetSchema", Map.of("version", "v2", "fields", List.of("id", "name", "middle_name", "phone_number", "age", "status")),
                        "sampleData", List.of(
                                Map.of("id", 1, "name", "Alice", "phone", "555-0101", "age", "30", "legacy_id", "L001"),
                                Map.of("id", 2, "name", "Bob", "phone", "555-0102", "age", "25", "legacy_id", "L002"),
                                Map.of("id", 3, "name", "Carol", "phone", "555-0103", "age", "35", "legacy_id", "L003")
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
