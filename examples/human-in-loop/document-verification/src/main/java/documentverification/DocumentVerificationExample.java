package documentverification;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import documentverification.workers.AiExtractWorker;
import documentverification.workers.StoreVerifiedWorker;

import java.util.List;
import java.util.Map;

/**
 * Document Verification -- AI Extract, Human Verify, Store
 *
 * Demonstrates Conductor's WAIT task for human-in-the-loop workflows:
 * 1. AI extracts data from a document (dv_ai_extract)
 * 2. Workflow pauses at a WAIT task for human verification
 * 3. Human verifies/corrects the extracted data and completes the WAIT task
 * 4. Verified data is stored (dv_store_verified)
 *
 * Run:
 *   java -jar target/document-verification-1.0.0.jar
 *   java -jar target/document-verification-1.0.0.jar --workers
 */
public class DocumentVerificationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Document Verification Demo: AI Extract + Human Verify ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef aiExtractTask = new TaskDef();
        aiExtractTask.setName("dv_ai_extract");
        aiExtractTask.setTimeoutSeconds(60);
        aiExtractTask.setResponseTimeoutSeconds(30);
        aiExtractTask.setOwnerEmail("examples@orkes.io");

        TaskDef storeVerifiedTask = new TaskDef();
        storeVerifiedTask.setName("dv_store_verified");
        storeVerifiedTask.setTimeoutSeconds(60);
        storeVerifiedTask.setResponseTimeoutSeconds(30);
        storeVerifiedTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(aiExtractTask, storeVerifiedTask));

        System.out.println("  Registered: dv_ai_extract, dv_store_verified\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'document_verification_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new AiExtractWorker(), new StoreVerifiedWorker());
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
        String workflowId = client.startWorkflow("document_verification_demo", 1,
                Map.of("documentId", "DOC-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion (workflow will pause at WAIT task)
        System.out.println("Step 5: Waiting for workflow (will pause at human verification WAIT task)...");
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
