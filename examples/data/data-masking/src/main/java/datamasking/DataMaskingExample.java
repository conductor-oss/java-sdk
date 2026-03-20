package datamasking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datamasking.workers.LoadRecordsWorker;
import datamasking.workers.DetectPiiWorker;
import datamasking.workers.MaskSsnWorker;
import datamasking.workers.MaskEmailPhoneWorker;
import datamasking.workers.EmitMaskedWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Masking Workflow Demo
 *
 * Demonstrates a PII masking pipeline:
 *   mk_load_records -> mk_detect_pii -> mk_mask_ssn -> mk_mask_email_phone -> mk_emit_masked
 *
 * Run:
 *   java -jar target/data-masking-1.0.0.jar
 */
public class DataMaskingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Masking Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mk_load_records", "mk_detect_pii", "mk_mask_ssn",
                "mk_mask_email_phone", "mk_emit_masked"));
        System.out.println("  Registered: mk_load_records, mk_detect_pii, mk_mask_ssn, mk_mask_email_phone, mk_emit_masked\n");

        System.out.println("Step 2: Registering workflow 'data_masking'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadRecordsWorker(),
                new DetectPiiWorker(),
                new MaskSsnWorker(),
                new MaskEmailPhoneWorker(),
                new EmitMaskedWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_masking", 1,
                Map.of("records", List.of(
                                Map.of("name", "Alice Smith", "ssn", "123-45-6789", "email", "alice@example.com", "phone", "555-123-4567"),
                                Map.of("name", "Bob Jones", "ssn", "987-65-4321", "email", "bob@company.com", "phone", "555-987-6543"),
                                Map.of("name", "Charlie Brown", "ssn", "456-78-9012", "email", "charlie@test.org", "phone", "555-456-7890")),
                        "maskingPolicy", "full"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
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
