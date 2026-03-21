package creditscoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import creditscoring.workers.CollectDataWorker;
import creditscoring.workers.CalculateFactorsWorker;
import creditscoring.workers.ScoreWorker;
import creditscoring.workers.ClassifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 492: Credit Scoring
 *
 * Collect credit data, calculate contributing factors,
 * compute a credit score, and classify the applicant.
 */
public class CreditScoringExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 492: Credit Scoring ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "csc_collect_data", "csc_calculate_factors", "csc_score", "csc_classify"));
        System.out.println("  Registered 4 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectDataWorker(),
                new CalculateFactorsWorker(),
                new ScoreWorker(),
                new ClassifyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("credit_scoring_workflow", 1,
                Map.of("applicantId", "APP-5501", "ssn", "XXX-XX-1234"));
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
