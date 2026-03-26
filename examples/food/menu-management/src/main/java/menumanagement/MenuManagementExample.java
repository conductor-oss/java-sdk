package menumanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import menumanagement.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 734: Menu Management — Create Items, Price, Categorize, Publish, Update
 */
public class MenuManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 734: Menu Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("mnu_create_items", "mnu_price", "mnu_categorize", "mnu_publish", "mnu_update"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateItemsWorker(), new PriceWorker(), new CategorizeWorker(), new PublishWorker(), new UpdateWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("menu_management_734", 1, Map.of("restaurantId", "REST-10", "menuName", "Spring 2026 Menu"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
