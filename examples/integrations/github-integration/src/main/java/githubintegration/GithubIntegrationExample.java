package githubintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import githubintegration.workers.ReceiveWebhookWorker;
import githubintegration.workers.CreatePrWorker;
import githubintegration.workers.RunChecksWorker;
import githubintegration.workers.MergePrWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 434: GitHub Integration
 *
 * Runs a GitHub integration workflow:
 * receive webhook -> create PR -> run checks -> merge.
 *
 * Run:
 *   java -jar target/github-integration-1.0.0.jar
 */
public class GithubIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 434: GitHub Integration ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gh_receive_webhook", "gh_create_pr", "gh_run_checks", "gh_merge_pr"));
        System.out.println("  Registered: gh_receive_webhook, gh_create_pr, gh_run_checks, gh_merge_pr\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'github_integration_434'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveWebhookWorker(),
                new CreatePrWorker(),
                new RunChecksWorker(),
                new MergePrWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("github_integration_434", 1,
                Map.of("repo", "acme/api-service",
                        "branch", "feature/new-endpoint",
                        "baseBranch", "main",
                        "commitMessage", "add user preferences endpoint"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  PR Number: " + workflow.getOutput().get("prNumber"));
        System.out.println("  Checks Passed: " + workflow.getOutput().get("checksPassed"));
        System.out.println("  Merged: " + workflow.getOutput().get("merged"));

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
