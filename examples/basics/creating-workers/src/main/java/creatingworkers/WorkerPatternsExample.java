package creatingworkers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import creatingworkers.workers.FetchDataWorker;
import creatingworkers.workers.SafeProcessWorker;
import creatingworkers.workers.SimpleTransformWorker;

import java.util.List;
import java.util.Map;

/**
 * Worker Patterns — Sync, Async, and Error Handling with Conductor
 *
 * Demonstrates three common worker patterns:
 * 1. SimpleTransformWorker — pure synchronous logic (text transform)
 * 2. FetchDataWorker — async I/O pattern (data fetch)
 * 3. SafeProcessWorker — error handling with FAILED status for Conductor retries
 *
 * The workflow chains them sequentially:
 *   simple_transform -> fetch_data -> safe_process
 *
 * Run:
 *   java -jar target/creating-workers-1.0.0.jar
 *   java -jar target/creating-workers-1.0.0.jar --workers
 */
public class WorkerPatternsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Worker Patterns: Sync, Async, and Error Handling ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("simple_transform", "fetch_data", "safe_process"));
        System.out.println("  Registered: simple_transform, fetch_data, safe_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'worker_demo_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SimpleTransformWorker(),
                new FetchDataWorker(),
                new SafeProcessWorker()
        );
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("worker_demo_workflow", 1,
                Map.of("text", "Hello Conductor", "source", "example-api"));
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
