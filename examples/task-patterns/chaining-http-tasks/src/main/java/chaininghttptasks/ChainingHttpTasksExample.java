package chaininghttptasks;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import chaininghttptasks.workers.PrepareRequestWorker;
import chaininghttptasks.workers.ProcessResponseWorker;

import java.util.List;
import java.util.Map;

/**
 * Chaining HTTP Tasks -- Sequential REST API Orchestration
 *
 * HTTP is a system task that makes REST API calls from the workflow.
 * No worker needed for HTTP tasks -- Conductor makes the call directly.
 *
 * This example demonstrates two patterns:
 *   Demo 1: Pure HTTP chain (POST to create, GET to verify, INLINE to format)
 *   Demo 2: Mixed SIMPLE + HTTP (worker prepares request, HTTP calls API, worker processes response)
 *
 * Uses Conductor's own API as the HTTP target for a self-contained demo.
 *
 * Run:
 *   java -jar target/chaining-http-tasks-1.0.0.jar
 */
public class ChainingHttpTasksExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Chaining HTTP Tasks: Sequential REST API Orchestration ===\n");
        System.out.println("HTTP tasks make REST calls from the Conductor server.");
        System.out.println("No worker needed -- the server handles it.\n");

        var client = new ConductorClientHelper();
        String conductorApiUrl = client.getServerUrl();

        // ---- Demo 1: Pure HTTP chain ----
        System.out.println("--- Demo 1: HTTP chain (register -> verify -> format) ---");

        // Register workflow (no task defs needed for HTTP/INLINE system tasks)
        System.out.println("Step 1: Registering workflow 'http_chain_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Starting workers for mixed workflow...\n");
            List<Worker> workers = List.of(new PrepareRequestWorker(), new ProcessResponseWorker());
            client.startWorkers(workers);
            System.out.println("  2 workers polling. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Start the HTTP chain workflow
        String taskName = "http_demo_task_" + System.currentTimeMillis();
        System.out.println("Step 2: Starting HTTP chain workflow with taskName=" + taskName + "...");
        String workflowId = client.startWorkflow("http_chain_demo", 1,
                Map.of("taskName", taskName, "conductorApiUrl", conductorApiUrl));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Wait for completion
        System.out.println("Step 3: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        if (workflow.getOutput() != null) {
            System.out.println("  Summary: " + workflow.getOutput().get("summary"));
        }
        System.out.println("");

        // ---- Demo 2: Mixed HTTP + SIMPLE ----
        System.out.println("--- Demo 2: SIMPLE -> HTTP -> SIMPLE ---");

        // Register task defs and mixed workflow
        System.out.println("Step 4: Registering task definitions and mixed workflow...");
        client.registerTaskDefs(List.of("http_prepare_request", "http_process_response"));
        client.registerWorkflow("mixed-workflow.json");
        System.out.println("  Task defs and workflow registered.\n");

        // Start workers
        System.out.println("Step 5: Starting workers...");
        List<Worker> workers = List.of(new PrepareRequestWorker(), new ProcessResponseWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        Thread.sleep(2000);

        // Start the mixed workflow
        System.out.println("Step 6: Starting mixed workflow...");
        String mixedId = client.startWorkflow("http_mixed_demo", 1,
                Map.of("query", "http_chain_demo", "conductorApiUrl", conductorApiUrl));
        System.out.println("  Workflow ID: " + mixedId + "\n");

        // Wait for completion
        System.out.println("Step 7: Waiting for completion...");
        Workflow mixedWorkflow = client.waitForWorkflow(mixedId, "COMPLETED", 30000);
        String mixedStatus = mixedWorkflow.getStatus().name();
        System.out.println("  Status: " + mixedStatus);
        if (mixedWorkflow.getOutput() != null) {
            System.out.println("  Result: " + mixedWorkflow.getOutput().get("processedResult"));
        }

        client.stopWorkers();

        System.out.println("\nHTTP task patterns:");
        System.out.println("  1. POST to create resources");
        System.out.println("  2. GET to read/verify");
        System.out.println("  3. Chain with INLINE for response processing");
        System.out.println("  4. Mix with SIMPLE workers for complex logic");

        boolean allPassed = "COMPLETED".equals(status) && "COMPLETED".equals(mixedStatus);
        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
