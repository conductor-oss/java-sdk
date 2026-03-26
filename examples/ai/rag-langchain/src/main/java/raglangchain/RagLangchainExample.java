package raglangchain;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import raglangchain.workers.LoadDocumentsWorker;
import raglangchain.workers.SplitTextWorker;
import raglangchain.workers.EmbedChunksWorker;
import raglangchain.workers.RetrieveWorker;
import raglangchain.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 165: RAG with LangChain Integration
 *
 * Implements a full RAG pipeline using LangChain-style components:
 * load documents, split text, embed chunks, retrieve relevant docs, and generate answers.
 *
 * Run:
 *   java -jar target/rag-langchain-1.0.0.jar
 *   java -jar target/rag-langchain-1.0.0.jar --workers
 */
public class RagLangchainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 165: RAG with LangChain Integration ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "lc_load_documents", "lc_split_text", "lc_embed_chunks",
                "lc_retrieve", "lc_generate"));
        System.out.println("  Registered: lc_load_documents, lc_split_text, lc_embed_chunks, lc_retrieve, lc_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_langchain'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadDocumentsWorker(),
                new SplitTextWorker(),
                new EmbedChunksWorker(),
                new RetrieveWorker(),
                new GenerateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_langchain", 1, Map.of(
                "sourceUrl", "https://example.com/langchain-docs",
                "chunkSize", 200,
                "chunkOverlap", 50,
                "question", "What is LangChain?",
                "topK", 3
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));
        System.out.println("  Chain: " + workflow.getOutput().get("chain"));
        System.out.println("  Tokens used: " + workflow.getOutput().get("tokensUsed"));

        System.out.println("\n--- RAG with LangChain Pattern ---");
        System.out.println("  - Load documents: Fetch content from source URL");
        System.out.println("  - Split text: Chunk documents using RecursiveCharacterTextSplitter");
        System.out.println("  - Embed chunks: Generate vector embeddings for each chunk");
        System.out.println("  - Retrieve: Find relevant docs using FAISS similarity search");
        System.out.println("  - Generate: Produce grounded answer using RetrievalQA chain");

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
