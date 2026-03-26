package policyissuance;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import policyissuance.workers.UnderwriteWorker;
import policyissuance.workers.ApproveWorker;
import policyissuance.workers.GeneratePolicyWorker;
import policyissuance.workers.IssueWorker;
import policyissuance.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 703: Policy Issuance
 */
public class PolicyIssuanceExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 703: Policy Issuance ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("pis_underwrite", "pis_approve", "pis_generate_policy", "pis_issue", "pis_deliver"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new UnderwriteWorker(), new ApproveWorker(), new GeneratePolicyWorker(), new IssueWorker(), new DeliverWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("pis_policy_issuance", 1, Map.of("applicantId", "APP-703", "coverageType", "auto", "requestedAmount", 100000));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("policyId: %s%n", workflow.getOutput().get("policyId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
