package eventnotification;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventnotification.workers.ParseEventWorker;
import eventnotification.workers.SendEmailWorker;
import eventnotification.workers.SendSmsWorker;
import eventnotification.workers.SendPushWorker;
import eventnotification.workers.RecordDeliveryWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Notification Demo
 *
 * Demonstrates a FORK_JOIN workflow that sends notifications across
 * email, SMS, and push channels in parallel:
 *   en_parse_event -> FORK(en_send_email, en_send_sms, en_send_push) -> JOIN -> en_record_delivery
 *
 * Run:
 *   java -jar target/event-notification-1.0.0.jar
 */
public class EventNotificationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Notification Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "en_parse_event", "en_send_email", "en_send_sms",
                "en_send_push", "en_record_delivery"));
        System.out.println("  Registered: en_parse_event, en_send_email, en_send_sms, en_send_push, en_record_delivery\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_notification'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseEventWorker(),
                new SendEmailWorker(),
                new SendSmsWorker(),
                new SendPushWorker(),
                new RecordDeliveryWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_notification", 1,
                Map.of("event", Map.of(
                                "type", "order.shipped",
                                "message", "Your order has been shipped!"),
                        "recipientId", "user-9001"));
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
