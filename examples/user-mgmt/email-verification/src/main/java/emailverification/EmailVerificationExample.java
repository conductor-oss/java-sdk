package emailverification;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import emailverification.workers.SendCodeWorker;
import emailverification.workers.WaitInputWorker;
import emailverification.workers.VerifyCodeWorker;
import emailverification.workers.ActivateAccountWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 604: Email Verification — Send Code, Wait, Verify, Activate
 *
 * Run:
 *   java -jar target/email-verification-1.0.0.jar
 */
public class EmailVerificationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 604: Email Verification ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("emv_send_code", "emv_wait_input", "emv_verify", "emv_activate"));
        System.out.println("  Registered: emv_send_code, emv_wait_input, emv_verify, emv_activate\n");

        System.out.println("Step 2: Registering workflow 'emv_email_verification'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SendCodeWorker(),
                new WaitInputWorker(),
                new VerifyCodeWorker(),
                new ActivateAccountWorker()
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
        String workflowId = client.startWorkflow("emv_email_verification", 1,
                Map.of("email", "diana@example.com", "userId", "USR-D4E5F6"));
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
