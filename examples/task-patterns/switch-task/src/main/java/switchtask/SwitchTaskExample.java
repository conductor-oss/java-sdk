package switchtask;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import switchtask.workers.AutoHandleWorker;
import switchtask.workers.TeamReviewWorker;
import switchtask.workers.EscalateWorker;
import switchtask.workers.UnknownPriorityWorker;
import switchtask.workers.LogActionWorker;

import java.util.List;
import java.util.Map;

/**
 * SWITCH Task — Conditional Routing by Value
 *
 * Demonstrates the SWITCH task with evaluatorType "value-param" for routing
 * based on ticket priority. Each priority level is handled by a different
 * worker, and all cases are followed by a logging step.
 *
 * Cases:
 * - LOW    -> sw_auto_handle  (auto-resolve)
 * - MEDIUM -> sw_team_review  (assign to support team)
 * - HIGH   -> sw_escalate     (escalate to manager)
 * - default-> sw_unknown_priority (needs classification)
 *
 * Run:
 *   java -jar target/switch-task-1.0.0.jar
 */
public class SwitchTaskExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SWITCH Task: Conditional Routing by Ticket Priority ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sw_auto_handle", "sw_team_review", "sw_escalate",
                "sw_unknown_priority", "sw_log_action"));
        System.out.println("  Registered: sw_auto_handle, sw_team_review, sw_escalate, sw_unknown_priority, sw_log_action\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'switch_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AutoHandleWorker(),
                new TeamReviewWorker(),
                new EscalateWorker(),
                new UnknownPriorityWorker(),
                new LogActionWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: LOW priority — auto-handled
        System.out.println("--- Scenario 1: LOW priority ticket ---");
        String wfId1 = client.startWorkflow("switch_demo", 1,
                Map.of("ticketId", "TKT-001", "priority", "LOW", "description", "Minor UI glitch"));
        System.out.println("  Workflow ID: " + wfId1);

        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        String status1 = wf1.getStatus().name();
        System.out.println("  Status: " + status1);
        System.out.println("  Output: " + wf1.getOutput());

        if (!"COMPLETED".equals(status1) || !Boolean.TRUE.equals(wf1.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED — expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: LOW priority ticket auto-handled and logged.\n");
        }

        // Scenario 2: MEDIUM priority — team review
        System.out.println("--- Scenario 2: MEDIUM priority ticket ---");
        String wfId2 = client.startWorkflow("switch_demo", 1,
                Map.of("ticketId", "TKT-002", "priority", "MEDIUM", "description", "Performance issue"));
        System.out.println("  Workflow ID: " + wfId2);

        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        String status2 = wf2.getStatus().name();
        System.out.println("  Status: " + status2);
        System.out.println("  Output: " + wf2.getOutput());

        if (!"COMPLETED".equals(status2) || !Boolean.TRUE.equals(wf2.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED — expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: MEDIUM priority ticket assigned to team and logged.\n");
        }

        // Scenario 3: HIGH priority — escalated
        System.out.println("--- Scenario 3: HIGH priority ticket ---");
        String wfId3 = client.startWorkflow("switch_demo", 1,
                Map.of("ticketId", "TKT-003", "priority", "HIGH", "description", "System outage"));
        System.out.println("  Workflow ID: " + wfId3);

        Workflow wf3 = client.waitForWorkflow(wfId3, "COMPLETED", 30000);
        String status3 = wf3.getStatus().name();
        System.out.println("  Status: " + status3);
        System.out.println("  Output: " + wf3.getOutput());

        if (!"COMPLETED".equals(status3) || !Boolean.TRUE.equals(wf3.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED — expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: HIGH priority ticket escalated and logged.\n");
        }

        // Scenario 4: Unknown priority — default case
        System.out.println("--- Scenario 4: CRITICAL priority (unknown — hits default) ---");
        String wfId4 = client.startWorkflow("switch_demo", 1,
                Map.of("ticketId", "TKT-004", "priority", "CRITICAL", "description", "Unknown severity"));
        System.out.println("  Workflow ID: " + wfId4);

        Workflow wf4 = client.waitForWorkflow(wfId4, "COMPLETED", 30000);
        String status4 = wf4.getStatus().name();
        System.out.println("  Status: " + status4);
        System.out.println("  Output: " + wf4.getOutput());

        if (!"COMPLETED".equals(status4) || !Boolean.TRUE.equals(wf4.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED — expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: unknown priority ticket flagged for classification and logged.\n");
        }

        // Summary
        System.out.println("Key insight: SWITCH with value-param routes based on the exact value");
        System.out.println("of the switchCaseValue input — no JavaScript evaluation needed.");

        client.stopWorkers();

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
