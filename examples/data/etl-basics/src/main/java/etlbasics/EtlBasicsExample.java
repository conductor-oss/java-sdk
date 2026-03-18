package etlbasics;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import etlbasics.workers.ExtractDataWorker;
import etlbasics.workers.TransformDataWorker;
import etlbasics.workers.ValidateOutputWorker;
import etlbasics.workers.LoadDataWorker;
import etlbasics.workers.ConfirmLoadWorker;

import java.util.List;
import java.util.Map;

/**
 * ETL Basics Demo
 *
 * Demonstrates a sequential ETL pipeline workflow:
 *   el_extract_data -> el_transform_data -> el_validate_output -> el_load_data -> el_confirm_load
 *
 * Run:
 *   java -jar target/etl-basics-1.0.0.jar
 */
public class EtlBasicsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== ETL Basics Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "el_extract_data", "el_transform_data",
                "el_validate_output", "el_load_data", "el_confirm_load"));
        System.out.println("  Registered: el_extract_data, el_transform_data, el_validate_output, el_load_data, el_confirm_load\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'etl_basics_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExtractDataWorker(),
                new TransformDataWorker(),
                new ValidateOutputWorker(),
                new LoadDataWorker(),
                new ConfirmLoadWorker()
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
        String workflowId = client.startWorkflow("etl_basics_wf", 1,
                Map.of("source", "customer-db",
                        "destination", "analytics-warehouse",
                        "rules", Map.of("trimNames", true, "lowercaseEmails", true)));
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
