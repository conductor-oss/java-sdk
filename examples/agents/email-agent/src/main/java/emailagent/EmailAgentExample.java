package emailagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import emailagent.workers.AnalyzeRequestWorker;
import emailagent.workers.DraftEmailWorker;
import emailagent.workers.ReviewToneWorker;
import emailagent.workers.SendEmailWorker;

import java.util.List;
import java.util.Map;

/**
 * Email Agent Demo
 *
 * Demonstrates a sequential pipeline of four workers that compose and send
 * an email: analyze request, draft email, review tone, and send.
 *   analyze_request -> draft_email -> review_tone -> send_email
 *
 * Run:
 *   java -jar target/email-agent-1.0.0.jar
 */
public class EmailAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Email Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ea_analyze_request", "ea_draft_email",
                "ea_review_tone", "ea_send_email"));
        System.out.println("  Registered: ea_analyze_request, ea_draft_email, ea_review_tone, ea_send_email\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'email_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalyzeRequestWorker(),
                new DraftEmailWorker(),
                new ReviewToneWorker(),
                new SendEmailWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("email_agent", 1,
                Map.of("intent", "Send a project update to the team lead about milestone completion",
                        "recipient", "sarah@company.com",
                        "context", "Project Alpha team lead. Milestone 3 completed 2 days ahead of schedule. All tests passing.",
                        "desiredTone", "professional"));
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
