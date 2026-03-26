package finetuneddeployment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import finetuneddeployment.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Fine-Tuned Model Deployment Pipeline
 *
 * Multi-step pipeline: validate the fine-tuned model, deploy to a
 * staging endpoint, run acceptance tests, then promote to production.
 * Demonstrates how Conductor orchestrates ML deployment workflows.
 *
 * Run:
 *   java -jar target/fine-tuned-deployment-1.0.0.jar
 */
public class FineTunedDeploymentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Fine-Tuned Model Deployment Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ftd_validate_model", "ftd_deploy_staging", "ftd_run_tests",
                "ftd_promote_production", "ftd_notify"));
        System.out.println("  Registered: 5 task definitions\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'fine_tuned_deploy_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateModelWorker(),
                new DeployStagingWorker(),
                new RunTestsWorker(),
                new PromoteProductionWorker(),
                new NotifyWorker());
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
        String workflowId = client.startWorkflow("fine_tuned_deploy_wf", 1,
                Map.of("modelId", "support-bot-v3",
                       "modelVersion", "3.1",
                       "baseModel", "llama-2-7b"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
