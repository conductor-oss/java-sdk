package abandonedcart;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import abandonedcart.workers.*;
import java.util.List;
import java.util.Map;

/** Example 465: Abandoned Cart Recovery */
public class AbandonedCartExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 465: Abandoned Cart Recovery ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("abc_detect_abandonment", "abc_send_reminder", "abc_offer_discount", "abc_convert"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetectAbandonmentWorker(), new SendReminderWorker(), new OfferDiscountWorker(), new ConvertWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("abandoned_cart_workflow", 1, Map.of("cartId", "CART-5512", "customerId", "CUST-4650", "cartTotal", 49.98));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        // Note: In a real scenario, the WAIT task would be completed externally
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
