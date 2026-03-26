package syncexecution;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import syncexecution.workers.AddWorker;

import java.util.List;
import java.util.Map;

/**
 * Synchronous Workflow Execution
 *
 * Demonstrates two ways to execute a workflow:
 *   1. Async -- start workflow, get ID, poll for result later
 *   2. Sync  -- start workflow and get result in the same HTTP call
 *
 * Workflow: sync_exec_demo
 *   [SIMPLE] sync_add -- adds two numbers
 *
 * Run:
 *   java -jar target/sync-execution-1.0.0.jar
 *   java -jar target/sync-execution-1.0.0.jar --workers
 */
public class SyncExecutionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 45: Synchronous Workflow Execution ===\n");
        System.out.println("Async: start workflow -> get ID -> poll for result");
        System.out.println("Sync:  start workflow -> get result in same call\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("sync_add"));
        System.out.println("  Registered: sync_add\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'sync_exec_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new AddWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Async execution (traditional)
        System.out.println("Step 4: Async execution (traditional)...");
        Map<String, Object> input = Map.of("a", 7, "b", 35);
        String workflowId = client.startWorkflow("sync_exec_demo", 1, input);
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  (Must poll to get result)\n");

        // Wait and get result
        Workflow asyncResult = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String asyncStatus = asyncResult.getStatus().name();
        System.out.println("  Async status: " + asyncStatus);
        System.out.println("  Async output: " + asyncResult.getOutput() + "\n");

        // Step 5 -- Sync execution (result in same call)
        System.out.println("Step 5: Sync execution (result in same call)...");
        System.out.println("  Input: a=10, b=32");
        Map<String, Object> syncInput = Map.of("a", 10, "b", 32);
        String syncWorkflowId = client.startWorkflow("sync_exec_demo", 1, syncInput);
        System.out.println("  Workflow ID: " + syncWorkflowId);
        Workflow syncResult = client.waitForWorkflow(syncWorkflowId, "COMPLETED", 30000);
        String syncStatus = syncResult.getStatus().name();
        System.out.println("  Sync status: " + syncStatus);
        System.out.println("  Sync output: " + syncResult.getOutput() + "\n");

        client.stopWorkers();

        if ("COMPLETED".equals(asyncStatus) && "COMPLETED".equals(syncStatus)) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
