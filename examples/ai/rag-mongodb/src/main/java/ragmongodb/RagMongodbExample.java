package ragmongodb;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragmongodb.workers.MongoEmbedWorker;
import ragmongodb.workers.MongoGenerateWorker;
import ragmongodb.workers.MongoVectorSearchWorker;

import java.util.List;
import java.util.Map;

/**
 * RAG with MongoDB Atlas Vector Search
 *
 * Demonstrates a 3-step RAG pipeline using MongoDB Atlas Vector Search:
 *   mongo_embed -> mongo_vector_search -> mongo_generate
 *
 * Workers perform $vectorSearch aggregation pipeline stage
 * with index configuration and filter expressions.
 *
 * Run:
 *   java -jar target/rag-mongodb-1.0.0.jar
 *   java -jar target/rag-mongodb-1.0.0.jar --workers
 */
public class RagMongodbExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== RAG with MongoDB Atlas Vector Search ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("mongo_embed", "mongo_vector_search", "mongo_generate"));
        System.out.println("  Registered: mongo_embed, mongo_vector_search, mongo_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_mongodb_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MongoEmbedWorker(),
                new MongoVectorSearchWorker(),
                new MongoGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_mongodb_workflow", 1,
                Map.of("question", "How does MongoDB Atlas Vector Search work?",
                       "database", "knowledge_base",
                       "collection", "articles"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n--- MongoDB Atlas Vector Search Features ---");
        System.out.println("  - $vectorSearch aggregation pipeline stage");
        System.out.println("  - Vector search indexes on embedding fields");
        System.out.println("  - Combine with standard MongoDB aggregation stages");

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
