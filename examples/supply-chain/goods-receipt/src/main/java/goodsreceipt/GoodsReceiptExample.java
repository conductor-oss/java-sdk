package goodsreceipt;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import goodsreceipt.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 655: Goods Receipt — Inbound Goods Processing
 */
public class GoodsReceiptExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 655: Goods Receipt ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "grc_receive", "grc_inspect", "grc_match_po", "grc_store", "grc_update_inventory"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveWorker(), new InspectWorker(), new MatchPoWorker(),
                new StoreWorker(), new UpdateInventoryWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop.\n"); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("grc_goods_receipt", 1,
                Map.of("shipmentId", "SHP-2024-655",
                       "poNumber", "PO-654-001",
                       "items", List.of(Map.of("sku", "BOLT-M10", "qty", 5000),
                                        Map.of("sku", "NUT-M10", "qty", 5000))));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
