package semistructuredrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import semistructuredrag.workers.ClassifyDataWorker;
import semistructuredrag.workers.SearchStructuredWorker;
import semistructuredrag.workers.SearchUnstructuredWorker;
import semistructuredrag.workers.MergeResultsWorker;
import semistructuredrag.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 158: Semi-Structured RAG
 *
 * Classifies input data into structured fields and unstructured text chunks,
 * searches both in parallel via FORK/JOIN, merges results, and generates
 * a final answer combining evidence from tables and documents.
 *
 * Run:
 *   java -jar target/semi-structured-rag-1.0.0.jar
 *   java -jar target/semi-structured-rag-1.0.0.jar --workers
 */
public class SemiStructuredRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 158: Semi-Structured RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ss_classify_data", "ss_search_structured",
                "ss_search_unstructured", "ss_merge_results", "ss_generate"));
        System.out.println("  Registered: ss_classify_data, ss_search_structured, ss_search_unstructured, ss_merge_results, ss_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'semi_structured_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyDataWorker(),
                new SearchStructuredWorker(),
                new SearchUnstructuredWorker(),
                new MergeResultsWorker(),
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

        // Step 4 — Start workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("semi_structured_rag_workflow", 1,
                Map.of("question", "What were the Q3 revenue figures and growth drivers?",
                        "dataContext", "financial reports and organizational data"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer"));
        System.out.println("  Total docs: " + wf.getOutput().get("totalDocs"));
        System.out.println("  Data types: " + wf.getOutput().get("dataTypes"));

        System.out.println("\n--- Semi-Structured RAG Pattern ---");
        System.out.println("  - Classify: Identify structured fields and unstructured chunks");
        System.out.println("  - Parallel search: Query tables and text simultaneously");
        System.out.println("  - Merge: Combine [TABLE] and [TEXT] evidence");
        System.out.println("  - Generate: Produce answer from unified context");

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
