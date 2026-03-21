package datalineage;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datalineage.workers.RegisterSourceWorker;
import datalineage.workers.ApplyTransform1Worker;
import datalineage.workers.ApplyTransform2Worker;
import datalineage.workers.RecordDestinationWorker;
import datalineage.workers.BuildLineageGraphWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Lineage Workflow Demo
 *
 * Demonstrates data lineage tracking through a pipeline:
 *   ln_register_source -> ln_apply_transform_1 -> ln_apply_transform_2 -> ln_record_destination -> ln_build_lineage_graph
 *
 * Run:
 *   java -jar target/data-lineage-1.0.0.jar
 */
public class DataLineageExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Lineage Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ln_register_source", "ln_apply_transform_1", "ln_apply_transform_2",
                "ln_record_destination", "ln_build_lineage_graph"));
        System.out.println("  Registered: ln_register_source, ln_apply_transform_1, ln_apply_transform_2, ln_record_destination, ln_build_lineage_graph\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_lineage'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RegisterSourceWorker(),
                new ApplyTransform1Worker(),
                new ApplyTransform2Worker(),
                new RecordDestinationWorker(),
                new BuildLineageGraphWorker()
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
        String workflowId = client.startWorkflow("data_lineage", 1,
                Map.of("records", List.of(
                        Map.of("id", 1, "name", "alice", "email", "ALICE@EXAMPLE.COM"),
                        Map.of("id", 2, "name", "bob", "email", "BOB@EXAMPLE.COM"),
                        Map.of("id", 3, "name", "charlie", "email", "CHARLIE@EXAMPLE.COM")
                ),
                "sourceName", "crm_postgres",
                "destName", "analytics_redshift"));
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
