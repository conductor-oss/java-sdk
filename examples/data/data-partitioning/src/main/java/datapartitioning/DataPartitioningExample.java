package datapartitioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datapartitioning.workers.SplitDataWorker;
import datapartitioning.workers.ProcessPartitionAWorker;
import datapartitioning.workers.ProcessPartitionBWorker;
import datapartitioning.workers.MergeResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Partitioning Workflow Demo
 *
 * Demonstrates a FORK_JOIN-based data partitioning workflow:
 *   par_split_data -> FORK(par_process_partition_a, par_process_partition_b) -> JOIN -> par_merge_results
 *
 * Run:
 *   java -jar target/data-partitioning-1.0.0.jar
 */
public class DataPartitioningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Partitioning Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "par_split_data", "par_process_partition_a",
                "par_process_partition_b", "par_merge_results"));
        System.out.println("  Registered: par_split_data, par_process_partition_a, par_process_partition_b, par_merge_results\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'data_partitioning_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SplitDataWorker(),
                new ProcessPartitionAWorker(),
                new ProcessPartitionBWorker(),
                new MergeResultsWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_partitioning_wf", 1,
                Map.of("records", List.of(
                                Map.of("id", 1, "value", "alpha"),
                                Map.of("id", 2, "value", "beta"),
                                Map.of("id", 3, "value", "gamma"),
                                Map.of("id", 4, "value", "delta"),
                                Map.of("id", 5, "value", "epsilon")),
                        "partitionKey", "id"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
