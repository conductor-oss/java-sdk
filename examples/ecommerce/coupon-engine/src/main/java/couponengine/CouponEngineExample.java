package couponengine;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import couponengine.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 460: Coupon Engine
 *
 * Performs a coupon engine workflow:
 * validate code -> check eligibility -> apply discount -> record usage.
 */
public class CouponEngineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 460: Coupon Engine ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("cpn_validate_code", "cpn_check_eligibility", "cpn_apply_discount", "cpn_record_usage"));
        System.out.println("  Registered 4 tasks.\n");

        System.out.println("Step 2: Registering workflow 'coupon_engine'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ValidateCodeWorker(), new CheckEligibilityWorker(), new ApplyDiscountWorker(), new RecordUsageWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n"); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("coupon_engine", 1, Map.of(
                "couponCode", "SUMMER20", "customerId", "cust-605", "cartTotal", 184.96,
                "cartItems", List.of(
                        Map.of("sku", "SKU-A1", "name", "Running Shoes", "price", 129.99, "qty", 1),
                        Map.of("sku", "SKU-B2", "name", "Athletic Socks", "price", 14.99, "qty", 2),
                        Map.of("sku", "SKU-C3", "name", "Water Bottle", "price", 24.99, "qty", 1))));
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
