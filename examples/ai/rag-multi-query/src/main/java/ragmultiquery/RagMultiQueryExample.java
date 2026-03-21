package ragmultiquery;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragmultiquery.workers.ExpandQueriesWorker;
import ragmultiquery.workers.SearchQ1Worker;
import ragmultiquery.workers.SearchQ2Worker;
import ragmultiquery.workers.SearchQ3Worker;
import ragmultiquery.workers.DedupResultsWorker;
import ragmultiquery.workers.GenerateAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 147: RAG with Multi-Query Expansion
 *
 * An LLM rephrases the user question into multiple search queries.
 * Each query searches the knowledge base in parallel. Results are
 * deduplicated, then the LLM generates a final answer.
 *
 * Pattern:
 *                          +-> search_q1 ->+
 *   expand_queries -> FORK -+-> search_q2 ->+-> JOIN -> dedup -> generate
 *                          +-> search_q3 ->+
 *
 * Run:
 *   java -jar target/rag-multi-query-1.0.0.jar
 *   java -jar target/rag-multi-query-1.0.0.jar --workers
 */
public class RagMultiQueryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 147: RAG with Multi-Query ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mq_expand_queries", "mq_search_q1", "mq_search_q2",
                "mq_search_q3", "mq_dedup_results", "mq_generate_answer"));
        System.out.println("  Registered: mq_expand_queries, mq_search_q1, mq_search_q2, mq_search_q3, mq_dedup_results, mq_generate_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_multi_query'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExpandQueriesWorker(),
                new SearchQ1Worker(),
                new SearchQ2Worker(),
                new SearchQ3Worker(),
                new DedupResultsWorker(),
                new GenerateAnswerWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_multi_query", 1,
                Map.of("question", "Why should I use workflow orchestration?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n  Question: " + workflow.getOutput().get("question"));
        System.out.println("  Expanded queries: " + workflow.getOutput().get("queries"));
        System.out.println("  Retrieved: " + workflow.getOutput().get("totalRetrieved") + " total, " + workflow.getOutput().get("uniqueDocs") + " unique");
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));

        System.out.println("\n--- RAG Multi-Query Pattern ---");
        System.out.println("  1. Expand: Rephrase question into multiple query variants");
        System.out.println("  2. Search: Run each query in parallel via FORK_JOIN");
        System.out.println("  3. Dedup: Merge and deduplicate retrieved documents");
        System.out.println("  4. Generate: LLM produces answer from unique context");

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
