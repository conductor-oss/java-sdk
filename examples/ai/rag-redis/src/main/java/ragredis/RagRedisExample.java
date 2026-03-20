package ragredis;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragredis.workers.RedisEmbedWorker;
import ragredis.workers.RedisFtSearchWorker;
import ragredis.workers.RedisGenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 138: RAG with Redis Vector Similarity Search
 *
 * Demonstrates RAG using Redis with RediSearch vector similarity.
 * Workers perform FT.SEARCH commands with HNSW index type
 * and KNN vector queries.
 *
 * Run:
 *   java -jar target/rag-redis-1.0.0.jar
 *   java -jar target/rag-redis-1.0.0.jar --workers
 */
public class RagRedisExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 138: RAG with Redis Vector Similarity ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("redis_embed", "redis_ft_search", "redis_generate"));
        System.out.println("  Registered: redis_embed, redis_ft_search, redis_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_redis_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RedisEmbedWorker(),
                new RedisFtSearchWorker(),
                new RedisGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_redis_workflow", 1,
                Map.of("question", "How does Redis handle vector search?",
                       "indexName", "idx:knowledge"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));

        @SuppressWarnings("unchecked")
        Map<String, Object> redisInfo = (Map<String, Object>) workflow.getOutput().get("redisInfo");
        if (redisInfo != null) {
            System.out.println("  Index: " + redisInfo.get("name")
                    + ", Type: " + redisInfo.get("type")
                    + ", Docs: " + redisInfo.get("numDocs"));
        }

        System.out.println("\n--- Redis Vector Similarity Features ---");
        System.out.println("  - FT.SEARCH with KNN clause (DIALECT 2)");
        System.out.println("  - HNSW (fast) and FLAT (exact) index types");
        System.out.println("  - Sub-millisecond latency for cached vectors");

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
