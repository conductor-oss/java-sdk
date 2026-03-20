package dataencryption;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataencryption.workers.GenerateKeyWorker;
import dataencryption.workers.IdentifyFieldsWorker;
import dataencryption.workers.EncryptFieldsWorker;
import dataencryption.workers.StoreKeyRefWorker;
import dataencryption.workers.VerifyEncryptedWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Encryption Workflow Demo
 *
 * Demonstrates an encryption pipeline:
 *   dn_generate_key -> dn_identify_fields -> dn_encrypt_fields -> dn_store_key_ref -> dn_verify_encrypted
 *
 * Run:
 *   java -jar target/data-encryption-1.0.0.jar
 */
public class DataEncryptionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Encryption Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dn_generate_key", "dn_identify_fields", "dn_encrypt_fields",
                "dn_store_key_ref", "dn_verify_encrypted"));
        System.out.println("  Registered: dn_generate_key, dn_identify_fields, dn_encrypt_fields, dn_store_key_ref, dn_verify_encrypted\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_encryption'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GenerateKeyWorker(),
                new IdentifyFieldsWorker(),
                new EncryptFieldsWorker(),
                new StoreKeyRefWorker(),
                new VerifyEncryptedWorker()
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
        String workflowId = client.startWorkflow("data_encryption", 1,
                Map.of("records", List.of(
                        Map.of("name", "Alice", "ssn", "123-45-6789", "creditCard", "4111111111111111", "email", "alice@example.com"),
                        Map.of("name", "Bob", "ssn", "987-65-4321", "creditCard", "5500000000000004", "email", "bob@example.com"),
                        Map.of("name", "Charlie", "ssn", "456-78-9012", "creditCard", "340000000000009", "email", "charlie@example.com")
                ),
                "algorithm", "AES-256-GCM",
                "sensitiveFields", List.of("ssn", "creditCard")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
