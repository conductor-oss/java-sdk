package warehousemanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import warehousemanagement.workers.*;
import java.util.*;

/**
 * Example 657: Warehouse Management — Receive to Ship Flow
 */
public class WarehouseManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 657: Warehouse Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("wm_receive","wm_put_away","wm_pick","wm_pack","wm_ship"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ReceiveWorker(), new PutAwayWorker(), new PickWorker(), new PackWorker(), new ShipWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("wm_warehouse_management", 1,
                Map.of("orderId","ORD-2024-657","items",List.of(Map.of("sku","WIDGET-A","qty",10),Map.of("sku","GADGET-B","qty",5),Map.of("sku","PART-C","qty",20)),"shippingMethod","express"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
