package ragreranking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragreranking.workers.EmbedWorker;
import ragreranking.workers.RetrieveWorker;
import ragreranking.workers.CrossEncoderWorker;
import ragreranking.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 145: RAG with Re-Ranking
 *
 * Adds a cross-encoder re-ranking step between retrieval and generation.
 * First retrieves a broad set of candidates, then re-ranks them with
 * a cross-encoder model for higher precision before generating.
 *
 * Run:
 *   java -jar target/rag-reranking-1.0.0.jar
 *   java -jar target/rag-reranking-1.0.0.jar --workers
 */
public class RagRerankingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 145: RAG with Re-Ranking ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rerank_embed", "rerank_retrieve", "rerank_crossencoder", "rerank_generate"));
        System.out.println("  Registered: rerank_embed, rerank_retrieve, rerank_crossencoder, rerank_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_reranking_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new EmbedWorker(),
                new RetrieveWorker(),
                new CrossEncoderWorker(),
                new GenerateWorker()
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
        String workflowId = client.startWorkflow("rag_reranking_workflow", 1,
                Map.of("question", "How does re-ranking improve RAG accuracy?",
                        "retrieveK", "6",
                        "rerankTopN", "3"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n  Answer: " + workflow.getOutput().get("answer"));
        System.out.println("  Retrieved: " + workflow.getOutput().get("retrievedCount") + " candidates");
        System.out.println("  Re-ranked to: " + workflow.getOutput().get("rerankedCount") + " top results");

        System.out.println("\n--- RAG Re-Ranking Pipeline ---");
        System.out.println("  1. Retrieve broadly (bi-encoder, fast but less precise)");
        System.out.println("  2. Re-rank narrowly (cross-encoder, slower but more accurate)");
        System.out.println("  3. Generate from top re-ranked results only");

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
