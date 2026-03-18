package leadscoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import leadscoring.workers.CollectSignalsWorker;
import leadscoring.workers.ScoreWorker;
import leadscoring.workers.ClassifyWorker;
import leadscoring.workers.RouteWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 622: Lead Scoring
 *
 * Demonstrates a lead scoring workflow:
 *   ls_collect_signals -> ls_score -> ls_classify -> ls_route
 *
 * Run:
 *   java -jar target/lead-scoring-1.0.0.jar
 */
public class LeadScoringExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 622: Lead Scoring ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ls_collect_signals", "ls_score", "ls_classify", "ls_route"));
        System.out.println("  Registered 4 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'ls_lead_scoring'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectSignalsWorker(),
                new ScoreWorker(),
                new ClassifyWorker(),
                new RouteWorker()
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
        String workflowId = client.startWorkflow("ls_lead_scoring", 1,
                Map.of("leadId", "LEAD-622", "email", "henry@techcorp.com", "company", "TechCorp"));
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
