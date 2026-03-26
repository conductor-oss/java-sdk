package multidocumentrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multidocumentrag.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 143: Multi-Document RAG -- Search Across Multiple Collections
 *
 * Uses a FORK/JOIN to search 3 vector collections in parallel,
 * merges results, then generates an answer from the combined context.
 * Demonstrates Conductor's parallel execution for multi-source RAG.
 *
 * Run:
 *   java -jar target/multi-document-rag-1.0.0.jar
 */
public class MultiDocumentRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 143: Multi-Document RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mdrag_embed", "mdrag_search_api_docs", "mdrag_search_tutorials",
                "mdrag_search_forums", "mdrag_merge_results", "mdrag_generate"));
        System.out.println("  Registered 6 task definitions.\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'multi_document_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new EmbedWorker(),
                new SearchApiDocsWorker(),
                new SearchTutorialsWorker(),
                new SearchForumsWorker(),
                new MergeResultsWorker(),
                new GenerateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("multi_document_rag_workflow", 1,
                Map.of("question", "How do I create and run a Conductor workflow?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));
        System.out.println("  Sources: " + workflow.getOutput().get("sourceCounts"));
        System.out.println("  Total results: " + workflow.getOutput().get("totalResults"));

        System.out.println("\n--- Multi-Document RAG with FORK/JOIN ---");
        System.out.println("  - FORK: Search multiple collections in parallel");
        System.out.println("  - Merge: Combine and re-rank results across sources");
        System.out.println("  - Generate: Unified answer from diverse knowledge bases");

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
