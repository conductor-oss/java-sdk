package privilegedaccess;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import privilegedaccess.workers.PamRequestWorker;
import privilegedaccess.workers.PamApproveWorker;
import privilegedaccess.workers.PamGrantAccessWorker;
import privilegedaccess.workers.PamRevokeAccessWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 391: Privileged Access -- Just-In-Time Privileged Access Management
 *
 * Pattern:
 *   request -> approve -> grant-access -> revoke-access
 *
 * Run:
 *   java -jar target/privileged-access-1.0.0.jar
 */
public class PrivilegedAccessExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 391: Privileged Access ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pam_request", "pam_approve", "pam_grant_access", "pam_revoke_access"));
        System.out.println("  Registered: pam_request, pam_approve, pam_grant_access, pam_revoke_access\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PamRequestWorker(),
                new PamApproveWorker(),
                new PamGrantAccessWorker(),
                new PamRevokeAccessWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("privileged_access_workflow", 1,
                Map.of("userId", "engineer-01",
                        "resource", "production-database",
                        "justification", "INC-2024-042 investigation",
                        "duration", "2h"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
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
