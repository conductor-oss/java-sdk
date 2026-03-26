package ragweaviate;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragweaviate.workers.WeavEmbedWorker;
import ragweaviate.workers.WeavSearchWorker;
import ragweaviate.workers.WeavGenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * RAG with Weaviate Vector Database — Conductor Workflow Example
 *
 * Demonstrates a three-step RAG pipeline:
 * 1. Embed the user question into a vector
 * 2. Search Weaviate for relevant documents
 * 3. Generate an answer from retrieved context
 *
 * Run:
 *   java -jar target/rag-weaviate-1.0.0.jar
 *   java -jar target/rag-weaviate-1.0.0.jar --workers
 */
public class RagWeaviateExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== RAG with Weaviate Vector Database Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("weav_embed", "weav_search", "weav_generate"));
        System.out.println("  Registered: weav_embed, weav_search, weav_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_weaviate_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WeavEmbedWorker(),
                new WeavSearchWorker(),
                new WeavGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_weaviate_workflow", 1,
                Map.of(
                        "question", "What is Weaviate and how does it handle vectorization?",
                        "className", "Document",
                        "properties", List.of("title", "content", "source"),
                        "limit", 3
                ));
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
