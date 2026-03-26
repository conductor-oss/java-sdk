package certificatemanagement;

import com.netflix.conductor.client.worker.Worker;
import certificatemanagement.workers.InventoryWorker;
import certificatemanagement.workers.AssessExpiryWorker;
import certificatemanagement.workers.RenewWorker;
import certificatemanagement.workers.DistributeWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 354: Certificate Management — Enterprise Certificate Lifecycle
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 354: Certificate Management ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "cm_inventory",
                "cm_assess_expiry",
                "cm_renew",
                "cm_distribute"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new InventoryWorker(),
                new AssessExpiryWorker(),
                new RenewWorker(),
                new DistributeWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("certificate_management_workflow", 1, Map.of(
                "scope", "all-environments",
                "renewalWindow", "30d"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  inventoryResult: " + execution.getOutput().get("inventoryResult"));
        System.out.println("  distributeResult: " + execution.getOutput().get("distributeResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
