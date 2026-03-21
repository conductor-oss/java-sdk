package eventtransformation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventtransformation.workers.ParseEventWorker;
import eventtransformation.workers.EnrichEventWorker;
import eventtransformation.workers.MapSchemaWorker;
import eventtransformation.workers.OutputEventWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Transformation Demo
 *
 * Demonstrates a sequential pipeline of four workers that transform raw events:
 * parse the event, enrich with context, map to CloudEvents schema, and deliver.
 *   et_parse_event -> et_enrich_event -> et_map_schema -> et_output_event
 *
 * Run:
 *   java -jar target/event-transformation-1.0.0.jar
 */
public class EventTransformationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Transformation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "et_parse_event", "et_enrich_event",
                "et_map_schema", "et_output_event"));
        System.out.println("  Registered: et_parse_event, et_enrich_event, et_map_schema, et_output_event\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_transformation_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseEventWorker(),
                new EnrichEventWorker(),
                new MapSchemaWorker(),
                new OutputEventWorker()
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
        String workflowId = client.startWorkflow("event_transformation_wf", 1,
                Map.of("rawEvent", Map.of(
                                "event_id", "raw-evt-fixed-001",
                                "event_type", "repository.push",
                                "ts", "2026-03-08T10:00:00Z",
                                "user_id", "U-5501",
                                "user_name", "jdoe",
                                "resource_type", "repository",
                                "resource_id", "repo-conductor-workflows",
                                "action", "push",
                                "metadata", Map.of("branch", "main", "commits", 3)),
                        "sourceFormat", "legacy_v1",
                        "targetFormat", "cloudevents"));
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
