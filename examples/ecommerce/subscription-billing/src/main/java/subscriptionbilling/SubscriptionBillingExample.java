package subscriptionbilling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import subscriptionbilling.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 459: Subscription Billing
 *
 * Performs a subscription billing workflow:
 * check billing period -> generate invoice -> charge -> activate next period.
 */
public class SubscriptionBillingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 459: Subscription Billing ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("sub_check_period", "sub_generate_invoice", "sub_charge", "sub_activate"));
        System.out.println("  Registered 4 tasks.\n");

        System.out.println("Step 2: Registering workflow 'subscription_billing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckPeriodWorker(), new GenerateInvoiceWorker(),
                new ChargeWorker(), new ActivateWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n"); Thread.currentThread().join(); return; }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("subscription_billing", 1, Map.of(
                "subscriptionId", "sub-1001", "customerId", "cust-501",
                "plan", "professional", "billingCycle", "monthly"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
