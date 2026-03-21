package searchagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import searchagent.workers.FormulateQueriesWorker;
import searchagent.workers.SearchGoogleWorker;
import searchagent.workers.SearchWikiWorker;
import searchagent.workers.RankMergeWorker;
import searchagent.workers.SynthesizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Search Agent Demo
 *
 * Demonstrates a search pipeline with parallel execution:
 *   formulate_queries -> FORK(search_google, search_wiki) -> JOIN -> rank_merge -> synthesize
 *
 * Run:
 *   java -jar target/search-agent-1.0.0.jar
 */
public class SearchAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Search Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sa_formulate_queries", "sa_search_google",
                "sa_search_wiki", "sa_rank_merge", "sa_synthesize"));
        System.out.println("  Registered: sa_formulate_queries, sa_search_google, sa_search_wiki, sa_rank_merge, sa_synthesize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'search_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new FormulateQueriesWorker(),
                new SearchGoogleWorker(),
                new SearchWikiWorker(),
                new RankMergeWorker(),
                new SynthesizeWorker()
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
        String workflowId = client.startWorkflow("search_agent", 1,
                Map.of("question", "What is the current state of quantum computing in 2026?",
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
