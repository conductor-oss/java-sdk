package externalpayload;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import externalpayload.workers.GenerateWorker;
import externalpayload.workers.ProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * External Payload Storage — Handle large data with chunking/reference pattern.
 *
 * Demonstrates how to handle large payloads in Conductor by returning
 * a summary and storage reference instead of the full data. The first
 * worker generates a summary + storageRef, and the second worker
 * processes just the summary.
 *
 * Run:
 *   java -jar target/external-payload-1.0.0.jar
 */
public class ExternalPayloadExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== External Payload Storage: Generate -> Process ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ep_generate", "ep_process"));
        System.out.println("  Registered: ep_generate, ep_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'large_payload_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GenerateWorker(),
                new ProcessWorker()
        );
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("large_payload_demo", 1,
                Map.of("dataSize", 10000));
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
