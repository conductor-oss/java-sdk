package insurancerenewal;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import insurancerenewal.workers.NotifyWorker;
import insurancerenewal.workers.ReviewWorker;
import insurancerenewal.workers.RepriceWorker;
import insurancerenewal.workers.ProcessRenewWorker;
import insurancerenewal.workers.ProcessCancelWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 878: Insurance Renewal
 */
public class InsuranceRenewalExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 878: Insurance Renewal ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("irn_notify", "irn_review", "irn_reprice", "irn_process_renew", "irn_process_cancel"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new NotifyWorker(), new ReviewWorker(), new RepriceWorker(), new ProcessRenewWorker(), new ProcessCancelWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("irn_insurance_renewal", 1, Map.of("policyId", "POL-878", "customerId", "CUST-878", "claimHistory", 1));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("decision: %s%n", workflow.getOutput().get("decision"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
