package marketplaceseller;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import marketplaceseller.workers.*;
import java.util.List;
import java.util.Map;

/** Example 467: Marketplace Seller Onboarding */
public class MarketplaceSellerExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 467: Marketplace Seller Onboarding ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mkt_onboard_seller", "mkt_verify_seller", "mkt_list_products", "mkt_manage_orders"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new OnboardSellerWorker(), new VerifySellerWorker(), new ListProductsWorker(), new ManageOrdersWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("marketplace_seller_workflow", 1, Map.of("sellerId", "SELLER-4401", "businessName", "Handmade Goods Co.", "category", "home_decor"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
