package dataanonymization;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataanonymization.workers.IdentifyPiiWorker;
import dataanonymization.workers.GeneralizeDataWorker;
import dataanonymization.workers.SuppressFieldsWorker;
import dataanonymization.workers.VerifyAnonymizationWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Anonymization Workflow Demo
 *
 * Demonstrates a data anonymization pipeline:
 *   an_identify_pii -> an_generalize_data -> an_suppress_fields -> an_verify_anonymization
 *
 * Run:
 *   java -jar target/data-anonymization-1.0.0.jar
 */
public class DataAnonymizationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Anonymization Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "an_identify_pii", "an_generalize_data",
                "an_suppress_fields", "an_verify_anonymization"));
        System.out.println("  Registered: an_identify_pii, an_generalize_data, an_suppress_fields, an_verify_anonymization\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_anonymization'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IdentifyPiiWorker(),
                new GeneralizeDataWorker(),
                new SuppressFieldsWorker(),
                new VerifyAnonymizationWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_anonymization", 1,
                Map.of("dataset", List.of(
                                Map.of("name", "Alice Johnson", "email", "alice@company.com", "ssn", "123-45-6789",
                                        "age", 34, "zipCode", "94103", "phone", "555-0101", "purchases", 12),
                                Map.of("name", "Bob Smith", "email", "bob@company.com", "ssn", "987-65-4321",
                                        "age", 28, "zipCode", "94107", "phone", "555-0102", "purchases", 8),
                                Map.of("name", "Carol Davis", "email", "carol@company.com", "ssn", "456-78-9012",
                                        "age", 45, "zipCode", "94110", "phone", "555-0103", "purchases", 22)),
                        "anonymizationLevel", "high"));
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
