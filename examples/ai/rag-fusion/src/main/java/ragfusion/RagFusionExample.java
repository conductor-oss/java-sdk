package ragfusion;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragfusion.workers.RewriteQueriesWorker;
import ragfusion.workers.SearchV1Worker;
import ragfusion.workers.SearchV2Worker;
import ragfusion.workers.SearchV3Worker;
import ragfusion.workers.FuseResultsWorker;
import ragfusion.workers.GenerateAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 151: RAG Fusion — Multi-Search Reciprocal Rank Fusion
 *
 * Rewrites the original query into multiple variants, searches in parallel
 * across three engines, fuses results using RRF scoring, and generates
 * a comprehensive answer from the fused context.
 *
 * Run:
 *   java -jar target/rag-fusion-1.0.0.jar
 *   java -jar target/rag-fusion-1.0.0.jar --workers
 */
public class RagFusionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 151: RAG Fusion ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rf_rewrite_queries", "rf_search_v1", "rf_search_v2",
                "rf_search_v3", "rf_fuse_results", "rf_generate_answer"));
        System.out.println("  Registered: rf_rewrite_queries, rf_search_v1, rf_search_v2, rf_search_v3, rf_fuse_results, rf_generate_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_fusion_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RewriteQueriesWorker(),
                new SearchV1Worker(),
                new SearchV2Worker(),
                new SearchV3Worker(),
                new FuseResultsWorker(),
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

        // Step 4 — Run RAG Fusion workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_fusion_workflow", 1,
                Map.of("question", "What features does Conductor offer?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer"));
        System.out.println("  Fused documents: " + wf.getOutput().get("fusedCount"));
        System.out.println("  Total candidates: " + wf.getOutput().get("totalCandidates"));

        System.out.println("\n--- RAG Fusion Pattern ---");
        System.out.println("  - Query rewriting: Generate multiple search perspectives");
        System.out.println("  - Parallel search: FORK across multiple search engines");
        System.out.println("  - RRF fusion: Reciprocal Rank Fusion combines ranked results");
        System.out.println("  - Answer generation: LLM synthesizes answer from fused context");

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
