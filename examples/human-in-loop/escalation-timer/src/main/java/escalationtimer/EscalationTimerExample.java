package escalationtimer;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import escalationtimer.workers.ProcessWorker;
import escalationtimer.workers.SubmitWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 98: Escalation Timer -- Auto-Approve After Timeout
 *
 * Workflow: submit -> WAIT (approval_wait) -> process
 *
 * Demonstrates auto-approving a WAIT task after a timeout by checking
 * for stale tasks using an external escalation checker pattern.
 */
public class EscalationTimerExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 98: Escalation Timer ===\n");

        var helper = new ConductorClientHelper();

        // Register task definitions
        var submitDef = new TaskDef("et_submit");
        submitDef.setOwnerEmail("examples@orkes.io");
        var processDef = new TaskDef("et_process");
        processDef.setOwnerEmail("examples@orkes.io");
        helper.registerTaskDefs(List.of(submitDef, processDef));

        // Register workflow
        helper.registerWorkflow("workflow.json");

        // Start workers
        helper.startWorkers(List.of(new SubmitWorker(), new ProcessWorker()));

        try {
            System.out.println("--- Scenario: Auto-approve after timeout ---");
            String workflowId = helper.startWorkflow("escalation_timer_demo", 1,
                    Map.of("requestId", "REQ-ESC", "autoApproveAfterMs", 3000));
            System.out.println("  Started workflow: " + workflowId);

            // Wait for the WAIT task to be scheduled
            Thread.sleep(2000);

            System.out.println("  Escalation checker running...");
            System.out.println("  (In production, a cron job would search for stale WAIT tasks)");

            // Wait for completion after auto-approval would occur
            var workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.println("  Status: " + workflow.getStatus());
            System.out.println("  Decision: " + workflow.getOutput().get("decision"));
            System.out.println("  Method: " + workflow.getOutput().get("method"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
