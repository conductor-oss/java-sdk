package sessionmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sessionmanagement.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 610: Session Management — Create, Validate, Refresh, Revoke
 */
public class SessionManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 610: Session Management ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ses_create", "ses_validate", "ses_refresh", "ses_revoke"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CreateSessionWorker(), new ValidateSessionWorker(), new RefreshSessionWorker(), new RevokeSessionWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("ses_session_management", 1,
                Map.of("userId", "USR-SES001", "deviceInfo", Map.of("browser", "Chrome", "os", "macOS", "ip", "192.168.1.42")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
