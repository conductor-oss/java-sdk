package gitopsworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gitopsworkflow.workers.DetectDriftWorker;
import gitopsworkflow.workers.PlanSyncWorker;
import gitopsworkflow.workers.ApplySyncWorker;
import gitopsworkflow.workers.VerifyStateWorker;

import java.util.List;
import java.util.Map;

/**
 * GitOps Workflow Demo -- Git-Driven Infrastructure Management
 *
 * Pattern:
 *   detect-drift -> plan-sync -> apply-sync -> verify-state
 *
 * Run:
 *   java -jar target/gitops-workflow-1.0.0.jar
 */
public class GitopsWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== GitOps Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "go_detect_drift", "go_plan_sync", "go_apply_sync", "go_verify_state"));
        System.out.println("  Registered: go_detect_drift, go_plan_sync, go_apply_sync, go_verify_state\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'gitops_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectDriftWorker(),
                new PlanSyncWorker(),
                new ApplySyncWorker(),
                new VerifyStateWorker()
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
        String workflowId = client.startWorkflow("gitops_workflow", 1,
                Map.of("repository", "infra-manifests",
                        "targetCluster", "prod-east"));
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
