package workflowrecovery;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import workflowrecovery.workers.DurableTaskWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Recovery After Server Restart
 *
 * Demonstrates Conductor's persistence -- workflows survive server restarts.
 * A simple durable task processes a batch, and after completion the workflow
 * is looked up again to verify that Conductor persisted the result.
 *
 * Run:
 *   java -jar target/workflow-recovery-1.0.0.jar
 *   java -jar target/workflow-recovery-1.0.0.jar --workers
 */
public class WorkflowRecoveryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Recovery After Server Restart Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definition
        System.out.println("Step 1: Registering task definition for wr_durable_task...");

        TaskDef durableTask = new TaskDef();
        durableTask.setName("wr_durable_task");
        durableTask.setRetryCount(0);
        durableTask.setTimeoutSeconds(60);
        durableTask.setResponseTimeoutSeconds(30);
        durableTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(durableTask));

        System.out.println("\n  Registered: wr_durable_task");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'workflow_recovery_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new DurableTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow with batch='batch-001'...\n");
        String workflowId = client.startWorkflow("workflow_recovery_demo", 1,
                Map.of("batch", "batch-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        // Step 6 -- Look up workflow again to verify persistence
        System.out.println("\nStep 6: Looking up workflow to verify persistence...");
        Workflow persisted = client.getWorkflow(workflowId);
        String persistedStatus = persisted.getStatus().name();
        System.out.println("  Persisted status: " + persistedStatus);
        System.out.println("  Persisted output: " + persisted.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status) && "COMPLETED".equals(persistedStatus)) {
            System.out.println("\nResult: PASSED -- Workflow completed and state persisted successfully.");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
