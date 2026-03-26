package humangroupclaim;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import humangroupclaim.workers.IntakeWorker;
import humangroupclaim.workers.ResolveWorker;

import java.util.List;
import java.util.Map;

/**
 * Group Assignment with Claim — Human-in-the-Loop pattern
 *
 * Demonstrates a workflow where a ticket is created (intake), then waits
 * for a human from an assigned group to claim and process it, and finally
 * the ticket is resolved.
 *
 * Workflow: intake -> WAIT (group-assigned task) -> resolve
 *
 * Run:
 *   java -jar target/human-group-claim-1.0.0.jar
 *   java -jar target/human-group-claim-1.0.0.jar --workers
 */
public class HumanGroupClaimExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Group Assignment with Claim: Human-in-the-Loop Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef intakeTask = new TaskDef();
        intakeTask.setName("hgc_intake");
        intakeTask.setRetryCount(0);
        intakeTask.setTimeoutSeconds(60);
        intakeTask.setResponseTimeoutSeconds(30);
        intakeTask.setOwnerEmail("examples@orkes.io");

        TaskDef resolveTask = new TaskDef();
        resolveTask.setName("hgc_resolve");
        resolveTask.setRetryCount(0);
        resolveTask.setTimeoutSeconds(60);
        resolveTask.setResponseTimeoutSeconds(30);
        resolveTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(intakeTask, resolveTask));

        System.out.println("  Registered: hgc_intake, hgc_resolve\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'human_group_claim'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new IntakeWorker(), new ResolveWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("human_group_claim", 1,
                Map.of(
                        "ticketId", "TKT-001",
                        "assignedGroup", "support-tier-2",
                        "instructions", "Review escalated customer issue"
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion (will pause at WAIT task)
        System.out.println("Step 5: Waiting for completion...");
        System.out.println("  (Workflow will pause at WAIT task until manually completed)");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: PAUSED (waiting for human claim)");
            System.exit(0);
        }
    }
}
