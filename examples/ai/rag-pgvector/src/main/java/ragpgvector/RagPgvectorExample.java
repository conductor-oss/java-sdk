package ragpgvector;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragpgvector.workers.PgvecEmbedWorker;
import ragpgvector.workers.PgvecGenerateWorker;
import ragpgvector.workers.PgvecQueryWorker;

import java.util.List;
import java.util.Map;

/**
 * RAG with PostgreSQL + pgvector
 *
 * Demonstrates RAG using PostgreSQL with the pgvector extension.
 * Workers perform SQL queries with vector similarity operators
 * ({@code <=>} for cosine, {@code <->} for L2, {@code <#>} for inner product).
 *
 * Run:
 *   java -jar target/rag-pgvector-1.0.0.jar
 *   java -jar target/rag-pgvector-1.0.0.jar --workers
 */
public class RagPgvectorExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== RAG with pgvector ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("pgvec_embed", "pgvec_query", "pgvec_generate"));
        System.out.println("  Registered: pgvec_embed, pgvec_query, pgvec_generate\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'rag_pgvector_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PgvecEmbedWorker(),
                new PgvecQueryWorker(),
                new PgvecGenerateWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_pgvector_workflow", 1,
                Map.of("question", "How does pgvector handle similarity search?",
                       "table", "knowledge_embeddings"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));
        System.out.println("  SQL: " + workflow.getOutput().get("sqlUsed"));

        System.out.println("\n--- pgvector-Specific Features ---");
        System.out.println("  - Native PostgreSQL extension (CREATE EXTENSION vector)");
        System.out.println("  - Operators: <=> cosine, <-> L2, <#> inner product");
        System.out.println("  - Index types: ivfflat (faster build) and hnsw (faster query)");

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
