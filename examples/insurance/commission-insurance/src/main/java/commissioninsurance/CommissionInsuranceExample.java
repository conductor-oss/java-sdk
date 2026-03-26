package commissioninsurance;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import commissioninsurance.workers.CalculateWorker;
import commissioninsurance.workers.ValidateWorker;
import commissioninsurance.workers.DeductAdvancesWorker;
import commissioninsurance.workers.PayWorker;
import commissioninsurance.workers.ReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 789: Commission (Insurance)
 */
public class CommissionInsuranceExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 789: Commission (Insurance) ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("cin_calculate", "cin_validate", "cin_deduct_advances", "cin_pay", "cin_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CalculateWorker(), new ValidateWorker(), new DeductAdvancesWorker(), new PayWorker(), new ReportWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("cin_commission_insurance", 1, Map.of("agentId", "AGT-789", "policyId", "POL-789", "premiumAmount", 3200));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("paymentId: %s%n", workflow.getOutput().get("paymentId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
