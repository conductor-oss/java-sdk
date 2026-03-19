package useronboarding;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import useronboarding.workers.CreateAccountWorker;
import useronboarding.workers.VerifyEmailWorker;
import useronboarding.workers.SetPreferencesWorker;
import useronboarding.workers.WelcomeWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 601: User Onboarding — Create Account, Verify, Set Preferences, Welcome
 *
 * Run:
 *   java -jar target/user-onboarding-1.0.0.jar
 */
public class UserOnboardingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 601: User Onboarding ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "uo_create_account", "uo_verify_email", "uo_set_preferences", "uo_welcome"));
        System.out.println("  Registered: uo_create_account, uo_verify_email, uo_set_preferences, uo_welcome\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'uo_user_onboarding'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateAccountWorker(),
                new VerifyEmailWorker(),
                new SetPreferencesWorker(),
                new WelcomeWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("uo_user_onboarding", 1,
                Map.of("username", "alice_johnson", "email", "alice@example.com", "fullName", "Alice Johnson", "plan", "pro"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
