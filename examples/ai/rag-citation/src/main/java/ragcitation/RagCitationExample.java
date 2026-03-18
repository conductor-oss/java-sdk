package ragcitation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragcitation.workers.RetrieveDocsWorker;
import ragcitation.workers.GenerateCitedWorker;
import ragcitation.workers.ExtractCitationsWorker;
import ragcitation.workers.VerifyCitationsWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 156: Citation RAG — Retrieve, Generate with Citations, Extract, and Verify
 *
 * Retrieves relevant documents, generates an answer with inline citation
 * markers, extracts citations from the answer, and verifies that all
 * cited document IDs exist in the retrieved document set.
 *
 * Run:
 *   java -jar target/rag-citation-1.0.0.jar
 *   java -jar target/rag-citation-1.0.0.jar --workers
 */
public class RagCitationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 156: Citation RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cr_retrieve_docs", "cr_generate_cited",
                "cr_extract_citations", "cr_verify_citations"));
        System.out.println("  Registered: cr_retrieve_docs, cr_generate_cited, cr_extract_citations, cr_verify_citations\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'citation_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RetrieveDocsWorker(),
                new GenerateCitedWorker(),
                new ExtractCitationsWorker(),
                new VerifyCitationsWorker()
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

        // Step 4 — Start workflow
        System.out.println("Step 4: Starting citation RAG workflow...\n");
        String workflowId = client.startWorkflow("citation_rag_workflow", 1,
                Map.of("question", "How does Conductor handle workflow execution and worker management?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer"));
        System.out.println("  Citations: " + wf.getOutput().get("citations"));
        System.out.println("  Extracted: " + wf.getOutput().get("extractedCitations"));
        System.out.println("  Verification: " + wf.getOutput().get("verificationResults"));

        System.out.println("\n--- Citation RAG Pattern ---");
        System.out.println("  - Retrieve docs: Fetch relevant documents with metadata");
        System.out.println("  - Generate cited: Produce answer with inline [n] markers");
        System.out.println("  - Extract citations: Validate markers appear in answer text");
        System.out.println("  - Verify citations: Confirm cited doc IDs exist in source set");

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
