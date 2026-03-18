package legalcontractreview;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import legalcontractreview.workers.LcrExtractTermsWorker;
import legalcontractreview.workers.LcrFinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Legal Contract Review -- AI Extract Terms, Human Legal Review, Finalize
 *
 * Demonstrates a workflow that:
 *   1. Extracts key terms from a contract (lcr_extract_terms)
 *   2. Waits for human legal review (WAIT task: legal_review)
 *   3. Finalizes the review (lcr_finalize) -- returns finalized=true
 *
 * Run:
 *   java -jar target/legal-contract-review-1.0.0.jar
 *   java -jar target/legal-contract-review-1.0.0.jar --workers
 */
public class LegalContractReviewExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Legal Contract Review Demo: AI Extract Terms + Human Legal Review ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef extractTermsTask = new TaskDef();
        extractTermsTask.setName("lcr_extract_terms");
        extractTermsTask.setTimeoutSeconds(60);
        extractTermsTask.setResponseTimeoutSeconds(30);
        extractTermsTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("lcr_finalize");
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(extractTermsTask, finalizeTask));

        System.out.println("  Registered: lcr_extract_terms, lcr_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'legal_contract_review_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new LcrExtractTermsWorker(), new LcrFinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("legal_contract_review_demo", 1,
                Map.of("contractId", "CONTRACT-2024-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion (WAIT task requires external signal)...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
