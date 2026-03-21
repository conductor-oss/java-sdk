package teamsintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import teamsintegration.workers.ReceiveWebhookWorker;
import teamsintegration.workers.FormatCardWorker;
import teamsintegration.workers.PostCardWorker;
import teamsintegration.workers.AcknowledgeWorker;

import java.util.List;
import java.util.Map;

/**
 * Microsoft Teams Integration Demo
 *
 * Performs a Teams integration workflow:
 *   tms_receive_webhook -> tms_format_card -> tms_post_card -> tms_acknowledge
 *
 * Run:
 *   java -jar target/teams-integration-1.0.0.jar
 */
public class TeamsIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Microsoft Teams Integration Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tms_receive_webhook", "tms_format_card",
                "tms_post_card", "tms_acknowledge"));
        System.out.println("  Registered: tms_receive_webhook, tms_format_card, tms_post_card, tms_acknowledge\n");

        System.out.println("Step 2: Registering workflow 'teams_integration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveWebhookWorker(),
                new FormatCardWorker(),
                new PostCardWorker(),
                new AcknowledgeWorker()
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
        String workflowId = client.startWorkflow("teams_integration", 1,
                Map.of("teamId", "team-eng-01",
                        "channelId", "ch-alerts",
                        "webhookPayload", Map.of(
                                "severity", "high",
                                "source", "monitoring",
                                "message", "CPU usage above 90%")));
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
