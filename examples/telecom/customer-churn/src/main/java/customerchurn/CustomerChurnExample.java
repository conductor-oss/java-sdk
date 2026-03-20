package customerchurn;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import customerchurn.workers.DetectRiskWorker;
import customerchurn.workers.AnalyzeReasonsWorker;
import customerchurn.workers.CreateOfferWorker;
import customerchurn.workers.DeliverWorker;
import customerchurn.workers.TrackWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 812: Customer Churn
 */
public class CustomerChurnExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 812: Customer Churn ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("ccn_detect_risk", "ccn_analyze_reasons", "ccn_create_offer", "ccn_deliver", "ccn_track"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetectRiskWorker(), new AnalyzeReasonsWorker(), new CreateOfferWorker(), new DeliverWorker(), new TrackWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("ccn_customer_churn", 1, Map.of("customerId", "CUST-812", "accountAge", 36, "usageTrend", "declining"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("retained: %s%n", workflow.getOutput().get("retained"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
