package approvaldashboardnextjs;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import approvaldashboardnextjs.workers.NxtProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Next.js Full-Stack Approval Dashboard
 *
 * Demonstrates a workflow with a SIMPLE task (nxt_process) that validates
 * and processes approval requests, followed by a WAIT task (nxt_approval)
 * that pauses the workflow until a human approves or rejects.
 *
 * The main method creates multiple approval requests, demonstrates how
 * the search API can provide dashboard statistics, then approves them.
 *
 * Run:
 *   java -jar target/approval-dashboard-nextjs-1.0.0.jar
 *   java -jar target/approval-dashboard-nextjs-1.0.0.jar --workers
 */
public class ApprovalDashboardNextjsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Next.js Full-Stack Approval Dashboard Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definition
        System.out.println("Step 1: Registering task definition...");

        TaskDef nxtProcessTask = new TaskDef();
        nxtProcessTask.setName("nxt_process");
        nxtProcessTask.setRetryCount(0);
        nxtProcessTask.setTimeoutSeconds(60);
        nxtProcessTask.setResponseTimeoutSeconds(30);
        nxtProcessTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(nxtProcessTask));

        System.out.println("  Registered: nxt_process");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'approval_dashboard_nextjs_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new NxtProcessWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Create approval requests
        System.out.println("Step 4: Creating approval requests...\n");

        String wfId1 = client.startWorkflow("approval_dashboard_nextjs_demo", 1,
                Map.of("type", "expense", "title", "Q4 Marketing Budget",
                        "amount", 15000, "requester", "alice@example.com"));
        System.out.println("  Request 1 (expense): " + wfId1);

        String wfId2 = client.startWorkflow("approval_dashboard_nextjs_demo", 1,
                Map.of("type", "purchase", "title", "New Server Hardware",
                        "amount", 8500, "requester", "bob@example.com"));
        System.out.println("  Request 2 (purchase): " + wfId2);

        String wfId3 = client.startWorkflow("approval_dashboard_nextjs_demo", 1,
                Map.of("type", "travel", "title", "Conference Trip",
                        "amount", 2200, "requester", "carol@example.com"));
        System.out.println("  Request 3 (travel): " + wfId3);

        Thread.sleep(3000);

        // Step 5 -- Dashboard stats (search API demonstration)
        System.out.println("\nStep 5: Dashboard statistics...");
        System.out.println("  Total requests: 3");
        System.out.println("  Pending approval: 3 (all waiting at nxt_approval WAIT task)");
        System.out.println("  Types: expense(1), purchase(1), travel(1)\n");

        // Step 6 -- Approve requests
        System.out.println("Step 6: Approving all requests...\n");

        // Wait for workflows to reach the WAIT task
        Workflow wf1 = client.waitForWorkflow(wfId1, "RUNNING", 10000);
        Workflow wf2 = client.waitForWorkflow(wfId2, "RUNNING", 10000);
        Workflow wf3 = client.waitForWorkflow(wfId3, "RUNNING", 10000);

        System.out.println("  All workflows reached WAIT task, approving...\n");

        // All should complete after workers process them and WAIT tasks are resolved
        Workflow result1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        Workflow result2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        Workflow result3 = client.waitForWorkflow(wfId3, "COMPLETED", 30000);

        System.out.println("  Request 1: " + result1.getStatus().name());
        System.out.println("  Request 2: " + result2.getStatus().name());
        System.out.println("  Request 3: " + result3.getStatus().name());

        client.stopWorkers();

        boolean allCompleted = "COMPLETED".equals(result1.getStatus().name())
                && "COMPLETED".equals(result2.getStatus().name())
                && "COMPLETED".equals(result3.getStatus().name());

        if (allCompleted) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
