package eventversioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventversioning.workers.DetectVersionWorker;
import eventversioning.workers.TransformV1Worker;
import eventversioning.workers.TransformV2Worker;
import eventversioning.workers.PassThroughWorker;
import eventversioning.workers.ProcessEventWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Versioning Demo
 *
 * Demonstrates a SWITCH-based event versioning workflow:
 *   vr_detect_version -> SWITCH(version:
 *       v1  -> vr_transform_v1,
 *       v2  -> vr_transform_v2,
 *       default -> vr_pass_through)
 *   -> vr_process_event
 *
 * Run:
 *   java -jar target/event-versioning-1.0.0.jar
 */
public class EventVersioningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Versioning Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "vr_detect_version", "vr_transform_v1",
                "vr_transform_v2", "vr_pass_through", "vr_process_event"));
        System.out.println("  Registered: vr_detect_version, vr_transform_v1, vr_transform_v2, vr_pass_through, vr_process_event\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'event_versioning'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectVersionWorker(),
                new TransformV1Worker(),
                new TransformV2Worker(),
                new PassThroughWorker(),
                new ProcessEventWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_versioning", 1,
                Map.of("event", Map.of(
                        "version", "v1",
                        "type", "user.created",
                        "name", "John",
                        "email", "john@example.com")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
