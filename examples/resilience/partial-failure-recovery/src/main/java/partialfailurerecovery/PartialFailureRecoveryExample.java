package partialfailurerecovery;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import partialfailurerecovery.workers.Step1Worker;
import partialfailurerecovery.workers.Step2Worker;
import partialfailurerecovery.workers.Step3Worker;

import java.util.List;
import java.util.Map;

/**
 * Partial Failure Recovery — Resume from Last Good State
 *
 * Demonstrates a three-step pipeline where step 2 fails on the first attempt.
 * The workflow is then retried using POST /workflow/{id}/retry, which resumes
 * execution from the failed task (step 2) rather than restarting from step 1.
 * Step 1's output is preserved and step 2 succeeds on the retry attempt.
 *
 * Run:
 *   java -jar target/partial-failure-recovery-1.0.0.jar
 *   java -jar target/partial-failure-recovery-1.0.0.jar --workers
 */
public class PartialFailureRecoveryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Partial Failure Recovery Demo: Resume from Last Good State ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef step1Def = new TaskDef();
        step1Def.setName("pfr_step1");
        step1Def.setRetryCount(0);
        step1Def.setTimeoutSeconds(60);
        step1Def.setResponseTimeoutSeconds(30);
        step1Def.setOwnerEmail("examples@orkes.io");

        TaskDef step2Def = new TaskDef();
        step2Def.setName("pfr_step2");
        step2Def.setRetryCount(0);
        step2Def.setTimeoutSeconds(60);
        step2Def.setResponseTimeoutSeconds(30);
        step2Def.setOwnerEmail("examples@orkes.io");

        TaskDef step3Def = new TaskDef();
        step3Def.setName("pfr_step3");
        step3Def.setRetryCount(0);
        step3Def.setTimeoutSeconds(60);
        step3Def.setResponseTimeoutSeconds(30);
        step3Def.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(step1Def, step2Def, step3Def));

        System.out.println("  Registered: pfr_step1, pfr_step2, pfr_step3");
        System.out.println("  Retry count: 0 (manual retry via API)\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'partial_failure_recovery_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new Step1Worker(), new Step2Worker(), new Step3Worker());
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start workflow (step 2 will fail on first attempt)
        System.out.println("Step 4: Starting workflow (data='hello')...\n");
        String workflowId = client.startWorkflow("partial_failure_recovery_demo", 1,
                Map.of("data", "hello"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for the workflow to fail (step 2 fails on first attempt)
        System.out.println("Step 5: Waiting for workflow to fail at step 2...");
        Workflow failedWorkflow = client.waitForWorkflow(workflowId, "FAILED", 30000);
        System.out.println("  Status: " + failedWorkflow.getStatus().name());
        System.out.println("  Step 1 completed successfully, step 2 failed.\n");

        // Step 6 — Retry the workflow (resumes from step 2, the last failed task)
        System.out.println("Step 6: Retrying workflow (POST /workflow/{id}/retry)...");
        System.out.println("  This resumes from step 2 -- step 1 is NOT re-executed.\n");
        client.retryWorkflow(workflowId);

        // Step 7 — Wait for completion
        System.out.println("Step 7: Waiting for completion...");
        Workflow completedWorkflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = completedWorkflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + completedWorkflow.getOutput());

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
