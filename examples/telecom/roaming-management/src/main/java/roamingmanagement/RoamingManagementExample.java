package roamingmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import roamingmanagement.workers.DetectRoamingWorker;
import roamingmanagement.workers.ValidateAgreementWorker;
import roamingmanagement.workers.RateWorker;
import roamingmanagement.workers.BillWorker;
import roamingmanagement.workers.SettleWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 820: Roaming Management
 */
public class RoamingManagementExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 820: Roaming Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("rmg_detect_roaming", "rmg_validate_agreement", "rmg_rate", "rmg_bill", "rmg_settle"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetectRoamingWorker(), new ValidateAgreementWorker(), new RateWorker(), new BillWorker(), new SettleWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("rmg_roaming_management", 1, Map.of("subscriberId", "SUB-820", "homeNetwork", "US-Mobile", "visitedNetwork", "DE-Telco"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("settled: %s%n", workflow.getOutput().get("settled"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
