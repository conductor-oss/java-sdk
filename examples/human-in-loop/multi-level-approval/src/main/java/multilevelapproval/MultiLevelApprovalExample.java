package multilevelapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import multilevelapproval.workers.FinalizeWorker;
import multilevelapproval.workers.SubmitWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Level Approval Chain: Manager -> Director -> VP
 *
 * Demonstrates a human-in-the-loop approval workflow where a request
 * must pass through three levels of approval. Each level uses a WAIT
 * task (pausing for human input) followed by a SWITCH task that checks
 * the approval decision. Rejection at any level terminates the workflow.
 *
 * Run:
 *   java -jar target/multi-level-approval-1.0.0.jar
 *   java -jar target/multi-level-approval-1.0.0.jar --workers
 */
public class MultiLevelApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Level Approval Chain Demo: Manager -> Director -> VP ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef submitTask = new TaskDef();
        submitTask.setName("mla_submit");
        submitTask.setRetryCount(0);
        submitTask.setTimeoutSeconds(60);
        submitTask.setResponseTimeoutSeconds(30);
        submitTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("mla_finalize");
        finalizeTask.setRetryCount(0);
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(submitTask, finalizeTask));

        System.out.println("  Registered: mla_submit, mla_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'multi_level_approval'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new SubmitWorker(), new FinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting multi-level approval workflow...\n");
        String workflowId = client.startWorkflow("multi_level_approval", 1,
                Map.of("requestId", "REQ-001", "requestor", "alice"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion (will stay in RUNNING due to WAIT tasks)
        System.out.println("Step 5: Workflow is now waiting for manager approval...");
        System.out.println("  Use the Conductor UI to complete WAIT tasks and perform approvals.\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "RUNNING", 10000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        client.stopWorkers();

        System.out.println("\nWorkflow is paused at first WAIT task (manager approval).");
        System.out.println("Complete WAIT tasks via Conductor API/UI to progress through the chain.");
        System.exit(0);
    }
}
