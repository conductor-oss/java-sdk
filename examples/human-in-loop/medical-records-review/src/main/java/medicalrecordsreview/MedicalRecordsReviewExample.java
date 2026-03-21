package medicalrecordsreview;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import medicalrecordsreview.workers.MrValidateHipaaWorker;
import medicalrecordsreview.workers.MrStoreResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Medical Records Review -- HIPAA Validation, Physician Review (WAIT), Store Result
 *
 * Demonstrates a human-in-the-loop workflow where:
 * 1. HIPAA compliance is validated automatically
 * 2. A physician reviews the medical records (WAIT task)
 * 3. The review result is stored with an audit trail
 *
 * Run:
 *   java -jar target/medical-records-review-1.0.0.jar
 *   java -jar target/medical-records-review-1.0.0.jar --workers
 */
public class MedicalRecordsReviewExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Medical Records Review Demo: HIPAA Validation + Physician Review ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef validateHipaaTask = new TaskDef();
        validateHipaaTask.setName("mr_validate_hipaa");
        validateHipaaTask.setTimeoutSeconds(60);
        validateHipaaTask.setResponseTimeoutSeconds(30);
        validateHipaaTask.setOwnerEmail("examples@orkes.io");

        TaskDef storeResultTask = new TaskDef();
        storeResultTask.setName("mr_store_result");
        storeResultTask.setTimeoutSeconds(60);
        storeResultTask.setResponseTimeoutSeconds(30);
        storeResultTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(validateHipaaTask, storeResultTask));

        System.out.println("  Registered: mr_validate_hipaa, mr_store_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'medical_records_review_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new MrValidateHipaaWorker(), new MrStoreResultWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("medical_records_review_demo", 1,
                Map.of("recordId", "REC-12345"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion (will pause at physician_review WAIT task)
        System.out.println("Step 5: Waiting for completion (will pause at physician_review WAIT task)...");
        System.out.println("  Complete the WAIT task externally to continue the workflow.\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
