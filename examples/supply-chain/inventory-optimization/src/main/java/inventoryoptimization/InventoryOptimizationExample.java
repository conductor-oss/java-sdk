package inventoryoptimization;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import inventoryoptimization.workers.*;
import java.util.*;

public class InventoryOptimizationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 660: Inventory Optimization ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("io_analyze_stock","io_calculate_reorder","io_optimize","io_execute"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AnalyzeStockWorker(), new CalculateReorderWorker(), new OptimizeWorker(), new ExecuteWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("io_inventory_optimization", 1,
                Map.of("warehouse","WH-Central","skuList",List.of("WIDGET-A","GADGET-B","PART-C","SENSOR-D","CABLE-E")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
