package userregistration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import userregistration.workers.ValidateWorker;
import userregistration.workers.CreateWorker;
import userregistration.workers.ConfirmWorker;
import userregistration.workers.ActivateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 602: User Registration — Validate, Create, Confirm, Activate
 *
 * Run:
 *   java -jar target/user-registration-1.0.0.jar
 */
public class UserRegistrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 602: User Registration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ur_validate", "ur_create", "ur_confirm", "ur_activate"));
        System.out.println("  Registered: ur_validate, ur_create, ur_confirm, ur_activate\n");

        System.out.println("Step 2: Registering workflow 'ur_user_registration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateWorker(),
                new CreateWorker(),
                new ConfirmWorker(),
                new ActivateWorker()
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
        String workflowId = client.startWorkflow("ur_user_registration", 1,
                Map.of("username", "bob_dev", "email", "bob@example.com", "password", "hashed_secret"));
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
