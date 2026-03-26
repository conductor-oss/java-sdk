package ragembeddingselection;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragembeddingselection.workers.PrepareBenchmarkWorker;
import ragembeddingselection.workers.EmbedOpenaiWorker;
import ragembeddingselection.workers.EmbedCohereWorker;
import ragembeddingselection.workers.EmbedLocalWorker;
import ragembeddingselection.workers.EvaluateEmbeddingsWorker;
import ragembeddingselection.workers.SelectBestWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 160: RAG Embedding Selection
 *
 * Benchmarks multiple embedding providers (OpenAI, Cohere, local) in parallel
 * using a FORK/JOIN pattern, evaluates their metrics, and selects the best
 * model based on a composite score.
 *
 * Run:
 *   java -jar target/rag-embedding-selection-1.0.0.jar
 *   java -jar target/rag-embedding-selection-1.0.0.jar --workers
 */
public class RagEmbeddingSelectionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 160: RAG Embedding Selection ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "es_prepare_benchmark", "es_embed_openai", "es_embed_cohere",
                "es_embed_local", "es_evaluate_embeddings", "es_select_best"));
        System.out.println("  Registered: es_prepare_benchmark, es_embed_openai, es_embed_cohere, es_embed_local, es_evaluate_embeddings, es_select_best\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_embedding_selection'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareBenchmarkWorker(),
                new EmbedOpenaiWorker(),
                new EmbedCohereWorker(),
                new EmbedLocalWorker(),
                new EvaluateEmbeddingsWorker(),
                new SelectBestWorker()
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
        String workflowId = client.startWorkflow("rag_embedding_selection", 1, Map.of());
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Best model: " + workflow.getOutput().get("bestModel"));
        System.out.println("  Best score: " + workflow.getOutput().get("bestScore"));
        System.out.println("  Recommendation: " + workflow.getOutput().get("recommendation"));

        System.out.println("\n--- RAG Embedding Selection Pattern ---");
        System.out.println("  - Prepare benchmark: Create queries and corpus");
        System.out.println("  - FORK: Evaluate OpenAI, Cohere, and local embeddings in parallel");
        System.out.println("  - JOIN: Collect all embedding metrics");
        System.out.println("  - Evaluate: Compute composite scores and rank models");
        System.out.println("  - Select: Choose the best model with recommendation");

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
