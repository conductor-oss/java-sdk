package ragmilvus;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragmilvus.workers.MilvusEmbedWorker;
import ragmilvus.workers.MilvusSearchWorker;
import ragmilvus.workers.MilvusGenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * RAG with Milvus — Retrieval-Augmented Generation Pipeline
 *
 * Demonstrates a three-step RAG pipeline using Milvus vector database:
 * 1. Embed the question into a vector
 * 2. Search Milvus collection for similar vectors
 * 3. Generate an answer from retrieved context
 *
 * Run:
 *   java -jar target/rag-milvus-1.0.0.jar
 *   java -jar target/rag-milvus-1.0.0.jar --workers
 */
public class RagMilvusExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== RAG with Milvus: Retrieval-Augmented Generation ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("milvus_embed", "milvus_search", "milvus_generate"));
        System.out.println("  Registered: milvus_embed, milvus_search, milvus_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_milvus_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MilvusEmbedWorker(),
                new MilvusSearchWorker(),
                new MilvusGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_milvus_workflow", 1,
                Map.of("question", "What index types does Milvus support?",
                       "collection", "tech_docs"));
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
