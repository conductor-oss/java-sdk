package batchscheduling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import batchscheduling.workers.PrioritizeJobsWorker;
import batchscheduling.workers.AllocateResourcesWorker;
import batchscheduling.workers.ExecuteBatchWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 404: Batch Scheduling
 *
 * Schedule batch jobs: prioritize tasks, allocate resources,
 * and execute the batch.
 *
 * Pattern:
 *   prioritize -> allocate -> execute
 *
 * Run:
 *   java -jar target/batch-scheduling-1.0.0.jar
 */
public class BatchSchedulingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 404: Batch Scheduling ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "bs_prioritize_jobs", "bs_allocate_resources", "bs_execute_batch"));
        System.out.println("  Registered: bs_prioritize_jobs, bs_allocate_resources, bs_execute_batch\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'batch_scheduling_404'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrioritizeJobsWorker(),
                new AllocateResourcesWorker(),
                new ExecuteBatchWorker());
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("batch_scheduling_404", 1,
                Map.of("batchId", "batch-20260308-001",
                        "jobs", List.of("etl-import", "data-transform", "report-gen"),
                        "maxConcurrency", 4));
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
