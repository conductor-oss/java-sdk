package structuredoutput;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import structuredoutput.workers.GenerateJsonWorker;
import structuredoutput.workers.TransformWorker;
import structuredoutput.workers.ValidateSchemaWorker;

import java.util.List;
import java.util.Map;

/**
 * Structured Output — Generate, validate, and transform structured JSON data.
 *
 * Pipeline: so_generate_json -> so_validate_schema -> so_transform
 *
 * Run:
 *   java -jar target/structured-output-1.0.0.jar
 */
public class StructuredOutputExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Structured Output: Generate, Validate & Transform JSON ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("so_generate_json", "so_validate_schema", "so_transform"));
        System.out.println("  Registered: so_generate_json, so_validate_schema, so_transform\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'structured_output_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GenerateJsonWorker(),
                new ValidateSchemaWorker(),
                new TransformWorker()
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
        String workflowId = client.startWorkflow("structured_output_workflow", 1,
                Map.of("entity", "company", "fields", List.of("name", "industry", "founded", "employees")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
