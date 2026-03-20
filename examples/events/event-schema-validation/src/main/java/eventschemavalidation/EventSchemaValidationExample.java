package eventschemavalidation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventschemavalidation.workers.ValidateSchemaWorker;
import eventschemavalidation.workers.ProcessValidWorker;
import eventschemavalidation.workers.DeadLetterWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Schema Validation Demo
 *
 * Demonstrates a SWITCH-based workflow that validates an incoming event against
 * a named schema, then routes it to either a processing task (valid) or a
 * dead-letter queue (invalid).
 *   sv_validate_schema -> SWITCH(result) -> sv_process_valid | sv_dead_letter
 *
 * Run:
 *   java -jar target/event-schema-validation-1.0.0.jar
 */
public class EventSchemaValidationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Schema Validation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sv_validate_schema", "sv_process_valid", "sv_dead_letter"));
        System.out.println("  Registered: sv_validate_schema, sv_process_valid, sv_dead_letter\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_schema_validation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateSchemaWorker(),
                new ProcessValidWorker(),
                new DeadLetterWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_schema_validation", 1,
                Map.of("event", Map.of(
                                "type", "order.created",
                                "source", "shop-api",
                                "data", Map.of("orderId", "ORD-100")),
                        "schemaName", "order_event_v1"));
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
