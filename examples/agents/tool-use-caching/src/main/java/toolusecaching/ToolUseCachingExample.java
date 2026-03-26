package toolusecaching;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import toolusecaching.workers.CheckCacheWorker;
import toolusecaching.workers.ReturnCachedWorker;
import toolusecaching.workers.ExecuteToolWorker;
import toolusecaching.workers.CacheResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Caching Demo
 *
 * Demonstrates checking a cache before executing a tool, and caching the
 * result afterward. Uses a SWITCH task to branch on cache hit vs miss:
 *   check_cache -> SWITCH(hit->return_cached, default/miss->execute_tool->cache_result)
 *
 * Run:
 *   java -jar target/tool-use-caching-1.0.0.jar
 */
public class ToolUseCachingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Caching Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "uc_check_cache", "uc_return_cached",
                "uc_execute_tool", "uc_cache_result"));
        System.out.println("  Registered: uc_check_cache, uc_return_cached, uc_execute_tool, uc_cache_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_caching'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckCacheWorker(),
                new ReturnCachedWorker(),
                new ExecuteToolWorker(),
                new CacheResultWorker()
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
        String workflowId = client.startWorkflow("tool_use_caching", 1,
                Map.of("toolName", "currency_exchange",
                        "toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 1000),
                        "cacheTtlSeconds", 300));
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
