package errornotification;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import errornotification.workers.ProcessOrderWorker;
import errornotification.workers.SendEmailWorker;
import errornotification.workers.SendPagerDutyWorker;
import errornotification.workers.SendSlackWorker;

import java.util.List;
import java.util.Map;

/**
 * Error Notification Example -- Slack/PagerDuty Alerts on Failure
 *
 * Demonstrates Conductor's failureWorkflow feature where a notification workflow
 * is automatically triggered when the main order processing workflow fails.
 *
 * The notification workflow uses FORK/JOIN to send Slack and Email alerts in parallel.
 *
 * Run:
 *   java -jar target/error-notification-1.0.0.jar
 *   java -jar target/error-notification-1.0.0.jar --workers
 */
public class ErrorNotificationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Error Notification Demo: Slack/PagerDuty Alerts on Failure ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef processOrderTask = new TaskDef();
        processOrderTask.setName("en_process_order");
        processOrderTask.setTimeoutSeconds(60);
        processOrderTask.setResponseTimeoutSeconds(30);
        processOrderTask.setOwnerEmail("examples@orkes.io");

        TaskDef sendSlackTask = new TaskDef();
        sendSlackTask.setName("en_send_slack");
        sendSlackTask.setTimeoutSeconds(60);
        sendSlackTask.setResponseTimeoutSeconds(30);
        sendSlackTask.setOwnerEmail("examples@orkes.io");

        TaskDef sendEmailTask = new TaskDef();
        sendEmailTask.setName("en_send_email");
        sendEmailTask.setTimeoutSeconds(60);
        sendEmailTask.setResponseTimeoutSeconds(30);
        sendEmailTask.setOwnerEmail("examples@orkes.io");

        TaskDef sendPagerDutyTask = new TaskDef();
        sendPagerDutyTask.setName("en_send_pagerduty");
        sendPagerDutyTask.setTimeoutSeconds(60);
        sendPagerDutyTask.setResponseTimeoutSeconds(30);
        sendPagerDutyTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(processOrderTask, sendSlackTask, sendEmailTask, sendPagerDutyTask));

        System.out.println("  Registered: en_process_order, en_send_slack, en_send_email, en_send_pagerduty\n");

        // Step 2 -- Register workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("notification-workflow.json");
        System.out.println("  Registered: error_notification (FORK/JOIN: Slack + Email)");
        client.registerWorkflow("workflow.json");
        System.out.println("  Registered: order_with_alerts (failureWorkflow = error_notification)\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessOrderWorker(),
                new SendSlackWorker(),
                new SendEmailWorker(),
                new SendPagerDutyWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start workflow that will succeed
        System.out.println("Step 4: Starting order workflow (shouldFail=false)...\n");
        String successId = client.startWorkflow("order_with_alerts", 1,
                Map.of("shouldFail", false));
        System.out.println("  Workflow ID: " + successId);

        Workflow successWf = client.waitForWorkflow(successId, "COMPLETED", 30000);
        System.out.println("  Status: " + successWf.getStatus().name());
        System.out.println("  Output: " + successWf.getOutput() + "\n");

        // Step 5 -- Start workflow that will fail (triggers notification workflow)
        System.out.println("Step 5: Starting order workflow (shouldFail=true)...\n");
        String failId = client.startWorkflow("order_with_alerts", 1,
                Map.of("shouldFail", true));
        System.out.println("  Workflow ID: " + failId);

        Workflow failWf = client.waitForWorkflow(failId, "FAILED", 30000);
        System.out.println("  Status: " + failWf.getStatus().name());
        System.out.println("  The failure workflow (error_notification) should be triggered automatically.\n");

        // Allow time for notification workflow to complete
        Thread.sleep(5000);

        client.stopWorkers();

        if ("COMPLETED".equals(successWf.getStatus().name())
                && "FAILED".equals(failWf.getStatus().name())) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
