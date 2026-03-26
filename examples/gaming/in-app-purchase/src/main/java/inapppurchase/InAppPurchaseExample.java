package inapppurchase;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import inapppurchase.workers.*;
import java.util.List;
import java.util.Map;
/** Example 744: In-App Purchase — Select Item, Verify, Charge, Deliver, Receipt */
public class InAppPurchaseExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 744: In-App Purchase ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("iap_select_item", "iap_verify", "iap_charge", "iap_deliver", "iap_receipt"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SelectItemWorker(), new VerifyWorker(), new ChargeWorker(), new DeliverWorker(), new ReceiptWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("in_app_purchase_744", 1, Map.of("playerId", "P-042", "itemId", "ITEM-DragonArmor", "price", 9.99));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
