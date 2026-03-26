package ragelasticsearch;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragelasticsearch.workers.EsEmbedWorker;
import ragelasticsearch.workers.EsKnnSearchWorker;
import ragelasticsearch.workers.EsGenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * RAG with Elasticsearch — Dense Vector knn Search
 *
 * Demonstrates RAG using Elasticsearch dense vector search (knn).
 * Workers perform ES knn queries with index/mapping configuration
 * and hybrid search combining knn with BM25 text scoring.
 *
 * Run:
 *   java -jar target/rag-elasticsearch-1.0.0.jar
 */
public class RagElasticsearchExample {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== RAG with Elasticsearch ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("es_embed", "es_knn_search", "es_generate"));
        System.out.println("  Registered: es_embed, es_knn_search, es_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_elasticsearch_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new EsEmbedWorker(),
                new EsKnnSearchWorker(),
                new EsGenerateWorker()
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
        String workflowId = client.startWorkflow("rag_elasticsearch_workflow", 1,
                Map.of("question", "How does Elasticsearch vector search work?",
                       "index", "knowledge-docs"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        Map<String, Object> output = workflow.getOutput();
        String answer = (String) output.get("answer");
        Map<String, Object> esStats = (Map<String, Object>) output.get("esStats");

        if (answer != null) {
            System.out.println("\n  Answer: " + answer);
        }
        if (esStats != null) {
            Map<String, Object> shards = (Map<String, Object>) esStats.get("shards");
            System.out.println("  Query took: " + esStats.get("took") + "ms, Shards: "
                    + shards.get("successful") + "/" + shards.get("total"));
        }

        System.out.println("\n--- Elasticsearch-Specific Features ---");
        System.out.println("  - dense_vector field type with knn search");
        System.out.println("  - num_candidates controls ANN accuracy vs speed");
        System.out.println("  - Hybrid search: knn + BM25 text scoring");

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
