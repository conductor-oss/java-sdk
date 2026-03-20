package kitchenworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import kitchenworkflow.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 735: Kitchen Workflow — Receive Order, Prep, Cook, Plate, Serve
 */
public class KitchenWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 735: Kitchen Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("kit_receive_order", "kit_prep", "kit_cook", "kit_plate", "kit_serve"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ReceiveOrderWorker(), new PrepWorker(), new CookWorker(), new PlateWorker(), new ServeWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("kitchen_workflow_735", 1,
                Map.of("orderId", "ORD-735", "tableId", "T-5", "items", List.of("Salmon", "Risotto", "Salad")));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
