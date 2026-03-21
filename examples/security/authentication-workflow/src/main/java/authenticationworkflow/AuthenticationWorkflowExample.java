package authenticationworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import authenticationworkflow.workers.ValidateCredentialsWorker;
import authenticationworkflow.workers.CheckMfaWorker;
import authenticationworkflow.workers.RiskAssessmentWorker;
import authenticationworkflow.workers.IssueTokenWorker;

import java.util.List;
import java.util.Map;

/**
 * Authentication Workflow Demo
 *
 * Demonstrates a multi-factor authentication orchestration:
 *   auth_validate_credentials -> auth_check_mfa -> auth_risk_assessment -> auth_issue_token
 *
 * Run:
 *   java -jar target/authentication-workflow-1.0.0.jar
 */
public class AuthenticationWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Authentication Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "auth_validate_credentials", "auth_check_mfa",
                "auth_risk_assessment", "auth_issue_token"));
        System.out.println("  Registered: auth_validate_credentials, auth_check_mfa, auth_risk_assessment, auth_issue_token\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'authentication_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateCredentialsWorker(),
                new CheckMfaWorker(),
                new RiskAssessmentWorker(),
                new IssueTokenWorker()
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
        String workflowId = client.startWorkflow("authentication_workflow", 1,
                Map.of("userId", "user-001",
                        "authMethod", "totp"));
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
