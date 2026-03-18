package basicrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import basicrag.workers.EmbedQueryWorker;
import basicrag.workers.SearchVectorsWorker;
import basicrag.workers.GenerateAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 131: Basic RAG — Retrieve Context, Generate Answer
 *
 * The foundational RAG pattern: embed a query, search a vector store
 * for relevant documents, then generate an answer using retrieved context.
 *
 * Run:
 *   java -jar target/basic-rag-1.0.0.jar
 *   java -jar target/basic-rag-1.0.0.jar --workers
 */
public class BasicRagExample {

    private static final long WAIT_TIMEOUT_MS = 30_000;

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        String embedModel = EmbedQueryWorker.configuredEmbedModel();
        String chatModel = GenerateAnswerWorker.configuredChatModel();

        System.out.println("=== Example 131: Basic RAG ===\n");

        System.out.println("Mode: LIVE embeddings (" + embedModel + ") + LIVE generation (" + chatModel + ") + TF-IDF search");
        System.out.println("  (vector search uses TF-IDF over bundled docs — see rag-pinecone/rag-chromadb/rag-pgvector for embedding-based search)\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("brag_embed_query", "brag_search_vectors", "brag_generate_answer"));
        System.out.println("  Registered: brag_embed_query, brag_search_vectors, brag_generate_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'basic_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new EmbedQueryWorker(),
                new SearchVectorsWorker(),
                new GenerateAnswerWorker()
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
        String workflowId = client.startWorkflow("basic_rag_workflow", 1,
                Map.of("question", "What is Conductor and how does RAG work?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", WAIT_TIMEOUT_MS);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("  Output: " + workflow.getOutput());
            System.out.println("\n  Question: " + workflow.getOutput().get("question"));
            System.out.println("  Answer: " + workflow.getOutput().get("answer"));

            System.out.println("\n--- Basic RAG Pattern ---");
            System.out.println("  1. Embed: Convert query to vector representation");
            System.out.println("  2. Search: Find semantically similar documents");
            System.out.println("  3. Generate: LLM produces answer grounded in retrieved context");
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            if (workflow.getReasonForIncompletion() != null && !workflow.getReasonForIncompletion().isBlank()) {
                System.out.println("  Workflow reason: " + workflow.getReasonForIncompletion());
            }
            if (workflow.getTasks() != null) {
                workflow.getTasks().stream()
                        .filter(t -> t.getStatus() != null
                                && (t.getStatus().name().equals("FAILED")
                                    || t.getStatus().name().equals("FAILED_WITH_TERMINAL_ERROR")))
                        .findFirst()
                        .ifPresent(t -> System.out.println("  Failed task: " + t.getTaskDefName()
                                + "\n  Reason: " + t.getReasonForIncompletion()));
            }
            System.exit(1);
        }
    }
}
