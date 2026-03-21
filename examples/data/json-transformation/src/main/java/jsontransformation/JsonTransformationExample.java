package jsontransformation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import jsontransformation.workers.ParseInputWorker;
import jsontransformation.workers.MapFieldsWorker;
import jsontransformation.workers.RestructureNestedWorker;
import jsontransformation.workers.ValidateSchemaWorker;
import jsontransformation.workers.EmitOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * JSON Transformation Demo
 *
 * Demonstrates a sequential JSON transformation pipeline:
 *   jt_parse_input -> jt_map_fields -> jt_restructure_nested -> jt_validate_schema -> jt_emit_output
 *
 * Run:
 *   java -jar target/json-transformation-1.0.0.jar
 */
public class JsonTransformationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 253: JSON Transformation ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "jt_parse_input", "jt_map_fields", "jt_restructure_nested",
                "jt_validate_schema", "jt_emit_output"));
        System.out.println("  Registered: jt_parse_input, jt_map_fields, jt_restructure_nested, jt_validate_schema, jt_emit_output\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'json_transformation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseInputWorker(),
                new MapFieldsWorker(),
                new RestructureNestedWorker(),
                new ValidateSchemaWorker(),
                new EmitOutputWorker()
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
        String workflowId = client.startWorkflow("json_transformation", 1,
                Map.of("sourceJson", Map.of(
                                "cust_id", "C-9001",
                                "first_name", "Jane",
                                "last_name", "Doe",
                                "email", "JANE.DOE@EXAMPLE.COM",
                                "phone", "+1-555-0199",
                                "reg_date", "2024-01-15",
                                "acct_type", "premium"),
                        "mappingRules", List.of("rename_fields", "normalize_case", "nest_structure")));
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
