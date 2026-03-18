package raghybridsearch;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import raghybridsearch.workers.GenerateAnswerWorker;
import raghybridsearch.workers.KeywordSearchWorker;
import raghybridsearch.workers.RrfMergeWorker;
import raghybridsearch.workers.VectorSearchWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 146: RAG with Hybrid Search (Vector + Keyword)
 *
 * Two parallel retrieval strategies: vector similarity search AND keyword
 * BM25 search. Results are merged using reciprocal rank fusion, then an
 * LLM generates the final answer from the fused context.
 *
 * Pattern:
 *              +-> vector_search ->+
 *   FORK_JOIN -|                   |-> JOIN -> rrf_merge -> generate_answer
 *              +-> keyword_search ->+
 *
 * Run:
 *   java -jar target/rag-hybrid-search-1.0.0.jar
 */
public class RagHybridSearchExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 146: RAG with Hybrid Search ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "hs_vector_search", "hs_keyword_search", "hs_rrf_merge", "hs_generate_answer"));
        System.out.println("  Registered: hs_vector_search, hs_keyword_search, hs_rrf_merge, hs_generate_answer\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'rag_hybrid_search'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new VectorSearchWorker(),
                new KeywordSearchWorker(),
                new RrfMergeWorker(),
                new GenerateAnswerWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_hybrid_search", 1,
                Map.of("question", "How does Conductor define and execute workflows?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();

        System.out.println("  Question: " + workflow.getOutput().get("question"));
        System.out.println("  Vector hits: " + workflow.getOutput().get("vectorCount")
                + ", Keyword hits: " + workflow.getOutput().get("keywordCount"));
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));

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
