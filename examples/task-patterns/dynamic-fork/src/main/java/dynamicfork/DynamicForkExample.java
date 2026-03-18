package dynamicfork;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dynamicfork.workers.PrepareTasksWorker;
import dynamicfork.workers.FetchUrlWorker;
import dynamicfork.workers.AggregateWorker;

import java.util.List;
import java.util.Map;

/**
 * Dynamic FORK (FORK_JOIN_DYNAMIC) — creates parallel branches at runtime
 *
 * Demonstrates dynamic fork execution in Conductor:
 * a prepare worker generates the task list at runtime, Conductor forks
 * them in parallel, then a join collects results for aggregation.
 *
 * Run:
 *   java -jar target/dynamic-fork-1.0.0.jar
 */
public class DynamicForkExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Dynamic FORK: Parallel URL Fetching ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("df_prepare_tasks", "df_fetch_url", "df_aggregate"));
        System.out.println("  Registered: df_prepare_tasks, df_fetch_url, df_aggregate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'dynamic_fork_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareTasksWorker(),
                new FetchUrlWorker(),
                new AggregateWorker()
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
        String workflowId = client.startWorkflow("dynamic_fork_demo", 1,
                Map.of("urls", List.of(
                        "https://example.com",
                        "https://openai.com",
                        "https://conductor.netflix.com"
                )));
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
