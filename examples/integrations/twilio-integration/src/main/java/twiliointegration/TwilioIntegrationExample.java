package twiliointegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import twiliointegration.workers.SendSmsWorker;
import twiliointegration.workers.WaitResponseWorker;
import twiliointegration.workers.ProcessResponseWorker;
import twiliointegration.workers.SendReplyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 436: Twilio Integration
 *
 * Performs a Twilio SMS integration workflow:
 * send SMS -> wait for response -> process response -> reply.
 *
 * Run:
 *   java -jar target/twilio-integration-1.0.0.jar
 */
public class TwilioIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        String sid = System.getenv("TWILIO_ACCOUNT_SID");
        String token = System.getenv("TWILIO_AUTH_TOKEN");
        boolean liveMode = sid != null && !sid.isBlank() && token != null && !token.isBlank();

        System.out.println("=== Example 436: Twilio Integration ===");
        System.out.println("Mode: " + (liveMode ? "LIVE (Twilio API)" : "DEMO (no Twilio credentials)"));
        System.out.println();

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "twl_send_sms", "twl_wait_response", "twl_process_response", "twl_send_reply"));
        System.out.println("  Registered: twl_send_sms, twl_wait_response, twl_process_response, twl_send_reply\n");

        System.out.println("Step 2: Registering workflow \'twilio_integration_436\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SendSmsWorker(),
                new WaitResponseWorker(),
                new ProcessResponseWorker(),
                new SendReplyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("twilio_integration_436", 1,
                Map.of("toNumber", "+15551234567",
                        "fromNumber", "+15559832143",
                        "messageBody", "Reply YES to confirm your appointment tomorrow at 2pm."));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Original SID: " + workflow.getOutput().get("originalSid"));
        System.out.println("  Reply SID: " + workflow.getOutput().get("replySid"));
        System.out.println("  Response: " + workflow.getOutput().get("responseReceived"));

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
