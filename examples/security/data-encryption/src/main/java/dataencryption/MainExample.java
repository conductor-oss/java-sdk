package dataencryption;

import com.netflix.conductor.client.worker.Worker;
import dataencryption.workers.ClassifyDataWorker;
import dataencryption.workers.GenerateKeyWorker;
import dataencryption.workers.EncryptDataWorker;
import dataencryption.workers.VerifyEncryptionWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 355: Data Encryption — Encryption Key and Data Protection Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 355: Data Encryption ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "de_classify_data",
                "de_generate_key",
                "de_encrypt_data",
                "de_verify_encryption"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ClassifyDataWorker(),
                new GenerateKeyWorker(),
                new EncryptDataWorker(),
                new VerifyEncryptionWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("data_encryption_workflow", 1, Map.of(
                "dataSource", "customer-database",
                "classification", "auto-detect"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  classify_dataResult: " + execution.getOutput().get("classify_dataResult"));
        System.out.println("  verify_encryptionResult: " + execution.getOutput().get("verify_encryptionResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
