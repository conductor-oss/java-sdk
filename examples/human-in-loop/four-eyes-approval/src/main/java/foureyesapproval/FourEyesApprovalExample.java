package foureyesapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import foureyesapproval.workers.FinalizeWorker;
import foureyesapproval.workers.SubmitWorker;

import java.util.List;
import java.util.Map;

/**
 * Four-Eyes Approval -- Two Independent Approvals Required
 *
 * Demonstrates a workflow where a submitted request must be approved
 * by two independent approvers before finalization. Uses FORK_JOIN
 * with two parallel WAIT tasks (one per approver) and a JOIN that
 * waits for both before proceeding to finalize.
 *
 * Run:
 *   java -jar target/four-eyes-approval-1.0.0.jar
 *   java -jar target/four-eyes-approval-1.0.0.jar --workers
 */
public class FourEyesApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Four-Eyes Approval Demo: Two Independent Approvals Required ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef submitTask = new TaskDef();
        submitTask.setName("fep_submit");
        submitTask.setTimeoutSeconds(60);
        submitTask.setResponseTimeoutSeconds(30);
        submitTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("fep_finalize");
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(submitTask, finalizeTask));

        System.out.println("  Registered: fep_submit, fep_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'four_eyes_approval_demo'...");
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
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("four_eyes_approval_demo", 1,
                Map.of("requestId", "REQ-001"));
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  Workflow will pause at two parallel WAIT tasks (approver_1, approver_2).");
        System.out.println("  Both must be completed externally for the workflow to proceed.\n");

        // Step 5 -- Wait for completion (will timeout since WAIT tasks need external completion)
        System.out.println("Step 5: Waiting for completion (WAIT tasks require external signals)...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 10000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nNote: Workflow is paused at WAIT tasks. Complete approver_1 and approver_2 externally.");
            System.exit(0);
        }
    }
}
