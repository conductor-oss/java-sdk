package ragcode;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragcode.workers.ParseQueryWorker;
import ragcode.workers.EmbedCodeQueryWorker;
import ragcode.workers.SearchCodeIndexWorker;
import ragcode.workers.GenerateCodeAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 153: Code RAG — Search and Answer from Code Index
 *
 * Parses a code question, embeds it for code search, retrieves
 * matching code snippets, and generates a code-aware answer.
 * Workers perform code parsing, embedding, indexing, and generation.
 *
 * Run:
 *   java -jar target/rag-code-1.0.0.jar
 *   java -jar target/rag-code-1.0.0.jar --workers
 */
public class RagCodeExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 153: Code RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cr_parse_query", "cr_embed_code_query",
                "cr_search_code_index", "cr_generate_code_answer"));
        System.out.println("  Registered: cr_parse_query, cr_embed_code_query, cr_search_code_index, cr_generate_code_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'code_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseQueryWorker(),
                new EmbedCodeQueryWorker(),
                new SearchCodeIndexWorker(),
                new GenerateCodeAnswerWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run code RAG query
        System.out.println("Step 4: Starting workflow — Code RAG query...\n");
        String workflowId = client.startWorkflow("code_rag_workflow", 1,
                Map.of("question", "How do I look up a user by ID?",
                        "language", "java"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer"));
        System.out.println("  Code Example: " + wf.getOutput().get("codeExample"));

        System.out.println("\n--- Code RAG Pattern ---");
        System.out.println("  - Parse query: Extract intent, keywords, and language");
        System.out.println("  - Embed code query: Create vector + code filter with AST node types");
        System.out.println("  - Search code index: Find matching snippets with file/line/signature");
        System.out.println("  - Generate code answer: Produce answer with inline code example");

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
