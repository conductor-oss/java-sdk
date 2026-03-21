package ragchromadb;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragchromadb.workers.ChromaEmbedWorker;
import ragchromadb.workers.ChromaGenerateWorker;
import ragchromadb.workers.ChromaQueryWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 134: RAG with ChromaDB (Local)
 *
 * Demonstrates RAG using a locally-running ChromaDB instance.
 * Workers perform ChromaDB collection queries with localhost
 * configuration and embedding functions.
 *
 * Run:
 *   java -jar target/rag-chromadb-1.0.0.jar
 *   java -jar target/rag-chromadb-1.0.0.jar --workers
 */
public class RagChromadbExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 134: RAG with ChromaDB (Local) ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("chroma_embed", "chroma_query", "chroma_generate"));
        System.out.println("  Registered: chroma_embed, chroma_query, chroma_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_chromadb_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ChromaEmbedWorker(),
                new ChromaQueryWorker(),
                new ChromaGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_chromadb_workflow", 1,
                Map.of("question", "How does ChromaDB store embeddings?",
                       "collection", "product_docs"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n--- ChromaDB-Specific Features ---");
        System.out.println("  - Runs locally on localhost:8000 or via Docker");
        System.out.println("  - Built-in default embedding function");
        System.out.println("  - Collection-based data organization");

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
