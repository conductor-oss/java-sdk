package profileupdate;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import profileupdate.workers.ValidateFieldsWorker;
import profileupdate.workers.UpdateProfileWorker;
import profileupdate.workers.SyncProfileWorker;
import profileupdate.workers.NotifyChangesWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 605: Profile Update — Validate, Update, Sync, Notify
 *
 * Run:
 *   java -jar target/profile-update-1.0.0.jar
 */
public class ProfileUpdateExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 605: Profile Update ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("pfu_validate", "pfu_update", "pfu_sync", "pfu_notify"));
        System.out.println("  Registered: pfu_validate, pfu_update, pfu_sync, pfu_notify\n");

        System.out.println("Step 2: Registering workflow 'pfu_profile_update'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateFieldsWorker(),
                new UpdateProfileWorker(),
                new SyncProfileWorker(),
                new NotifyChangesWorker()
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
        String workflowId = client.startWorkflow("pfu_profile_update", 1,
                Map.of("userId", "USR-X7Y8Z9",
                       "updates", Map.of("displayName", "Eve Martinez", "bio", "Software engineer", "location", "San Francisco")));
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
