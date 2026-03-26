package ragqdrant;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragqdrant.workers.QdrantEmbedWorker;
import ragqdrant.workers.QdrantSearchWorker;
import ragqdrant.workers.QdrantGenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * RAG with Qdrant — Retrieval-Augmented Generation with Payload Filtering
 *
 * Demonstrates a three-step RAG pipeline:
 * 1. Embed the question into a vector
 * 2. Search Qdrant with payload filtering and score threshold
 * 3. Generate an answer from retrieved context
 *
 * Run:
 *   java -jar target/rag-qdrant-1.0.0.jar
 *   java -jar target/rag-qdrant-1.0.0.jar --workers
 */
public class RagQdrantExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== RAG with Qdrant: Retrieval-Augmented Generation ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("qdrant_embed", "qdrant_search", "qdrant_generate"));
        System.out.println("  Registered: qdrant_embed, qdrant_search, qdrant_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_qdrant_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new QdrantEmbedWorker(),
                new QdrantSearchWorker(),
                new QdrantGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_qdrant_workflow", 1,
                Map.of("question", "How does Qdrant filter during search?",
                       "collection", "knowledge"));
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
