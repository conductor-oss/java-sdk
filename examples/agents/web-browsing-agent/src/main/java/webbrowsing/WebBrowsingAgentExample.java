package webbrowsing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webbrowsing.workers.PlanSearchWorker;
import webbrowsing.workers.ExecuteSearchWorker;
import webbrowsing.workers.SelectPagesWorker;
import webbrowsing.workers.ReadPageWorker;
import webbrowsing.workers.ExtractAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Web Browsing Agent Demo
 *
 * Demonstrates a sequential pipeline of five workers that perform web browsing
 * to answer a question:
 *   plan_search -> execute_search -> select_pages -> read_page -> extract_answer
 *
 * Run:
 *   java -jar target/web-browsing-agent-1.0.0.jar
 */
public class WebBrowsingAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Web Browsing Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wb_plan_search", "wb_execute_search", "wb_select_pages",
                "wb_read_page", "wb_extract_answer"));
        System.out.println("  Registered: wb_plan_search, wb_execute_search, wb_select_pages, wb_read_page, wb_extract_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'web_browsing_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PlanSearchWorker(),
                new ExecuteSearchWorker(),
                new SelectPagesWorker(),
                new ReadPageWorker(),
                new ExtractAnswerWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("web_browsing_agent", 1,
                Map.of("question", "What are the key features and production capabilities of Conductor workflow engine?",
                        "maxPages", 3));
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
