package customeronboardingkyc;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import customeronboardingkyc.workers.KycActivateWorker;
import customeronboardingkyc.workers.KycCheckWorker;

import java.util.List;
import java.util.Map;

/**
 * Customer Onboarding KYC -- Auto/Human Review
 *
 * Demonstrates a workflow that:
 *   1. Runs automated KYC check (kyc_check)
 *   2. Uses SWITCH to route high-risk customers to manual review (WAIT task)
 *   3. Auto-approves low-risk customers (skips WAIT)
 *   4. Activates the customer account (kyc_activate)
 *
 * Run:
 *   java -jar target/customer-onboarding-kyc-1.0.0.jar
 *   java -jar target/customer-onboarding-kyc-1.0.0.jar --workers
 */
public class CustomerOnboardingKycExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Customer Onboarding KYC Demo: Auto/Human Review ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef kycCheckTask = new TaskDef();
        kycCheckTask.setName("kyc_check");
        kycCheckTask.setTimeoutSeconds(60);
        kycCheckTask.setResponseTimeoutSeconds(30);
        kycCheckTask.setOwnerEmail("examples@orkes.io");

        TaskDef kycActivateTask = new TaskDef();
        kycActivateTask.setName("kyc_activate");
        kycActivateTask.setTimeoutSeconds(60);
        kycActivateTask.setResponseTimeoutSeconds(30);
        kycActivateTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(kycCheckTask, kycActivateTask));

        System.out.println("  Registered: kyc_check, kyc_activate\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'customer_onboarding_kyc'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new KycCheckWorker(), new KycActivateWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start a low-risk workflow (auto-approved)
        System.out.println("Step 4: Starting workflow (low-risk customer, auto-approved)...\n");
        String workflowId = client.startWorkflow("customer_onboarding_kyc", 1,
                Map.of("customerId", "C-1001", "customerName", "Alice Smith", "riskLevel", "low"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
