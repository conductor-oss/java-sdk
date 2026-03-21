package notificationpreferences;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import notificationpreferences.workers.LoadPrefsWorker;
import notificationpreferences.workers.UpdatePrefsWorker;
import notificationpreferences.workers.SyncChannelsWorker;
import notificationpreferences.workers.ConfirmPrefsWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 606: Notification Preferences — Load, Update, Sync Channels, Confirm
 *
 * Run:
 *   java -jar target/notification-preferences-1.0.0.jar
 */
public class NotificationPreferencesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 606: Notification Preferences ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("np_load", "np_update", "np_sync_channels", "np_confirm"));
        System.out.println("  Registered: np_load, np_update, np_sync_channels, np_confirm\n");

        System.out.println("Step 2: Registering workflow 'np_notification_preferences'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadPrefsWorker(),
                new UpdatePrefsWorker(),
                new SyncChannelsWorker(),
                new ConfirmPrefsWorker()
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
        String workflowId = client.startWorkflow("np_notification_preferences", 1,
                Map.of("userId", "USR-NP001", "preferences", Map.of("sms", true, "slack", true)));
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
