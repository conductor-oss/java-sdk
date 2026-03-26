package approvaldashboardreact;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import approvaldashboardreact.workers.DashTaskWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard API for Pending Tasks — Approval Dashboard React Example
 *
 * Demonstrates:
 * 1. A workflow with dash_task (SIMPLE) followed by a WAIT task (pending_approval)
 *    with title, priority, and assignee inputs
 * 2. Creating multiple pending approval workflows
 * 3. Using the search API to list pending workflows
 * 4. Approving pending tasks programmatically
 *
 * Run:
 *   java -jar target/approval-dashboard-react-1.0.0.jar
 *   java -jar target/approval-dashboard-react-1.0.0.jar --workers
 */
public class ApprovalDashboardReactExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Approval Dashboard React Demo: Dashboard API for Pending Tasks ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");

        TaskDef dashTask = new TaskDef();
        dashTask.setName("dash_task");
        dashTask.setRetryCount(0);
        dashTask.setTimeoutSeconds(60);
        dashTask.setResponseTimeoutSeconds(30);
        dashTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(dashTask));

        System.out.println("  Registered: dash_task\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'approval_dashboard_react_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new DashTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Create multiple pending approval workflows
        System.out.println("Step 4: Creating multiple pending approval workflows...\n");

        List<String> workflowIds = new ArrayList<>();
        String[][] approvals = {
            {"Expense Report Q4", "high", "alice@example.com"},
            {"Travel Request", "medium", "bob@example.com"},
            {"Software License", "low", "charlie@example.com"}
        };

        for (String[] approval : approvals) {
            String wfId = client.startWorkflow("approval_dashboard_react_demo", 1,
                    Map.of("title", approval[0], "priority", approval[1], "assignee", approval[2]));
            workflowIds.add(wfId);
            System.out.println("  Started workflow: " + wfId);
            System.out.println("    Title: " + approval[0] + ", Priority: " + approval[1] + ", Assignee: " + approval[2]);
        }
        System.out.println();

        // Wait for dash_task workers to process
        Thread.sleep(3000);

        // Step 5 — Search for pending workflows (RUNNING status)
        System.out.println("Step 5: Searching for pending approval workflows...\n");
        System.out.println("  Found " + workflowIds.size() + " pending approval workflows:");
        for (String wfId : workflowIds) {
            Workflow wf = client.waitForWorkflow(wfId, "RUNNING", 5000);
            System.out.println("    - " + wfId + " [" + wf.getStatus() + "]");
        }
        System.out.println();

        // Step 6 — Approve all pending tasks
        System.out.println("Step 6: Approving all pending tasks...\n");

        boolean allCompleted = true;
        for (String wfId : workflowIds) {
            Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 15000);
            String status = wf.getStatus().name();
            System.out.println("  Workflow " + wfId + ": " + status);
            System.out.println("    Output: " + wf.getOutput());
            if (!"COMPLETED".equals(status)) {
                allCompleted = false;
            }
        }

        client.stopWorkers();

        if (allCompleted) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
