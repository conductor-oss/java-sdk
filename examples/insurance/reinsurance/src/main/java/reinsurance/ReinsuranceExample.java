package reinsurance;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import reinsurance.workers.AssessRiskWorker;
import reinsurance.workers.TreatyLookupWorker;
import reinsurance.workers.CedeWorker;
import reinsurance.workers.ConfirmWorker;
import reinsurance.workers.ReconcileWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 705: Reinsurance
 */
public class ReinsuranceExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 705: Reinsurance ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("rin_assess_risk", "rin_treaty_lookup", "rin_cede", "rin_confirm", "rin_reconcile"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AssessRiskWorker(), new TreatyLookupWorker(), new CedeWorker(), new ConfirmWorker(), new ReconcileWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("rin_reinsurance", 1, Map.of("policyId", "POL-705", "coverageAmount", 5000000, "riskCategory", "property-catastrophe"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("cessionId: %s%n", workflow.getOutput().get("cessionId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
