package approvaldelegation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import approvaldelegation.workers.PrepareWorker;
import approvaldelegation.workers.FinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Approval Delegation — Reassign Approval to Another Person
 *
 * Demonstrates a workflow where an initial approver can either approve directly
 * or delegate the approval to another person. Uses WAIT tasks for human input
 * and a SWITCH task to route based on the action taken.
 *
 * Flow: prepare -> WAIT (initial approval) -> SWITCH on action:
 *   - "delegate" -> new WAIT (delegated approval) -> finalize
 *   - default (approve) -> finalize
 *
 * Run:
 *   java -jar target/approval-delegation-1.0.0.jar
 *   java -jar target/approval-delegation-1.0.0.jar --workers
 */
public class ApprovalDelegationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Approval Delegation Demo: Reassign Approval to Another Person ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareTask = new TaskDef();
        prepareTask.setName("ad_prepare");
        prepareTask.setTimeoutSeconds(60);
        prepareTask.setResponseTimeoutSeconds(30);
        prepareTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("ad_finalize");
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareTask, finalizeTask));
        System.out.println("  Registered: ad_prepare, ad_finalize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'approval_delegation_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PrepareWorker(), new FinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("approval_delegation_demo", 1, Map.of());
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion (approval via external signal)...");
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
