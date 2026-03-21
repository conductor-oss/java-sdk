package toolusesequential;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import toolusesequential.workers.SearchWebWorker;
import toolusesequential.workers.ReadPageWorker;
import toolusesequential.workers.ExtractDataWorker;
import toolusesequential.workers.SummarizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Sequential Demo
 *
 * Demonstrates a sequential pipeline of four deterministic.tool calls that search
 * the web, read a page, extract structured data, and produce a summary:
 *   search_web -> read_page -> extract_data -> summarize
 *
 * Run:
 *   java -jar target/tool-use-sequential-1.0.0.jar
 */
public class ToolUseSequentialExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Sequential Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ts_search_web", "ts_read_page", "ts_extract_data", "ts_summarize"));
        System.out.println("  Registered: ts_search_web, ts_read_page, ts_extract_data, ts_summarize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_sequential'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SearchWebWorker(),
                new ReadPageWorker(),
                new ExtractDataWorker(),
                new SummarizeWorker()
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
        String workflowId = client.startWorkflow("tool_use_sequential", 1,
                Map.of("query", "What is Conductor workflow orchestration?",
                        "maxResults", 5));
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
