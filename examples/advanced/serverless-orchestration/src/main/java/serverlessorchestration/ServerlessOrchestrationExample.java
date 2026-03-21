package serverlessorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import serverlessorchestration.workers.SvlInvokeParseWorker;
import serverlessorchestration.workers.SvlInvokeEnrichWorker;
import serverlessorchestration.workers.SvlInvokeScoreWorker;
import serverlessorchestration.workers.SvlAggregateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 561: Serverless Orchestration
 *
 * Invoke serverless functions, chain their outputs, and
 * aggregate the final results.
 *
 * Run:
 *   java -jar target/serverless-orchestration-1.0.0.jar
 */
public class ServerlessOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 561: Serverless Orchestration ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "svl_invoke_parse", "svl_invoke_enrich",
                "svl_invoke_score", "svl_aggregate"));
        System.out.println("  Registered: svl_invoke_parse, svl_invoke_enrich, svl_invoke_score, svl_aggregate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'serverless_orchestration_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SvlInvokeParseWorker(),
                new SvlInvokeEnrichWorker(),
                new SvlInvokeScoreWorker(),
                new SvlAggregateWorker()
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
        String workflowId = client.startWorkflow("serverless_orchestration_demo", 1,
                Map.of("eventId", "EVT-99001",
                        "payload", Map.of("action", "page_view")));
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
