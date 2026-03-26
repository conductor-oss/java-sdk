package slackintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import slackintegration.workers.ReceiveEventWorker;
import slackintegration.workers.ProcessEventWorker;
import slackintegration.workers.PostMessageWorker;
import slackintegration.workers.TrackDeliveryWorker;

import java.util.List;
import java.util.Map;

/**
 * Slack Integration Demo
 *
 * Performs a Slack integration workflow:
 *   slk_receive_event -> slk_process_event -> slk_post_message -> slk_track_delivery
 *
 * Run:
 *   java -jar target/slack-integration-1.0.0.jar
 */
public class SlackIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Slack Integration Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "slk_receive_event", "slk_process_event",
                "slk_post_message", "slk_track_delivery"));
        System.out.println("  Registered: slk_receive_event, slk_process_event, slk_post_message, slk_track_delivery\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'slack_integration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveEventWorker(),
                new ProcessEventWorker(),
                new PostMessageWorker(),
                new TrackDeliveryWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("slack_integration", 1,
                Map.of("channel", "engineering",
                        "eventType", "deployment",
                        "payload", Map.of(
                                "service", "api-gateway",
                                "version", "2.1.0")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
