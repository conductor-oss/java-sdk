package donormanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import donormanagement.workers.*;
import java.util.List;
import java.util.Map;
/** Example 755: Donor Management — Acquire, Steward, Acknowledge, Retain, Upgrade */
public class DonorManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 755: Donor Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("dnr_acquire", "dnr_steward", "dnr_acknowledge", "dnr_retain", "dnr_upgrade"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AcquireWorker(), new StewardWorker(), new AcknowledgeWorker(), new RetainWorker(), new UpgradeWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("donor_management_755", 1, Map.of("donorName", "Robert Chen", "donorEmail", "robert@example.com", "firstGift", 500));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
