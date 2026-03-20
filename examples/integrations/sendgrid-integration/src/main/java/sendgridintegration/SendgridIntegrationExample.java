package sendgridintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sendgridintegration.workers.ComposeEmailWorker;
import sendgridintegration.workers.PersonalizeWorker;
import sendgridintegration.workers.SendEmailWorker;
import sendgridintegration.workers.TrackOpensWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 437: SendGrid Integration
 *
 * Performs a SendGrid email integration workflow:
 * compose email -> personalize -> send -> track opens.
 *
 * Run:
 *   java -jar target/sendgrid-integration-1.0.0.jar
 */
public class SendgridIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 437: SendGrid Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sgd_compose_email", "sgd_personalize", "sgd_send_email", "sgd_track_opens"));
        System.out.println("  Registered: sgd_compose_email, sgd_personalize, sgd_send_email, sgd_track_opens\n");

        System.out.println("Step 2: Registering workflow \'sendgrid_integration_437\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ComposeEmailWorker(),
                new PersonalizeWorker(),
                new SendEmailWorker(),
                new TrackOpensWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("sendgrid_integration_437", 1,
                Map.of("recipientEmail", "user@example.com",
                        "recipientName", "Alice",
                        "templateId", "tmpl-welcome-001",
                        "campaignId", "camp-onboard-q1"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Message ID: " + workflow.getOutput().get("messageId"));
        System.out.println("  Delivered: " + workflow.getOutput().get("delivered"));
        System.out.println("  Tracking Enabled: " + workflow.getOutput().get("trackingEnabled"));

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
