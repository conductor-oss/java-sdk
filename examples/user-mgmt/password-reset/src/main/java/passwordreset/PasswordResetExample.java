package passwordreset;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import passwordreset.workers.RequestWorker;
import passwordreset.workers.VerifyTokenWorker;
import passwordreset.workers.ResetWorker;
import passwordreset.workers.NotifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 603: Password Reset — Request, Verify, Reset, Notify
 *
 * Run:
 *   java -jar target/password-reset-1.0.0.jar
 */
public class PasswordResetExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 603: Password Reset ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("pwd_request", "pwd_verify", "pwd_reset", "pwd_notify"));
        System.out.println("  Registered: pwd_request, pwd_verify, pwd_reset, pwd_notify\n");

        System.out.println("Step 2: Registering workflow 'pwd_password_reset'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RequestWorker(),
                new VerifyTokenWorker(),
                new ResetWorker(),
                new NotifyWorker()
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
        String workflowId = client.startWorkflow("pwd_password_reset", 1,
                Map.of("email", "carol@example.com", "newPassword", "S3cure!Pass#2026"));
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
