package endorsementprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import endorsementprocessing.workers.RequestChangeWorker;
import endorsementprocessing.workers.AssessWorker;
import endorsementprocessing.workers.PriceWorker;
import endorsementprocessing.workers.ApproveWorker;
import endorsementprocessing.workers.ApplyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 704: Endorsement Processing
 */
public class EndorsementProcessingExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 704: Endorsement Processing ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("edp_request_change", "edp_assess", "edp_price", "edp_approve", "edp_apply"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RequestChangeWorker(), new AssessWorker(), new PriceWorker(), new ApproveWorker(), new ApplyWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("edp_endorsement_processing", 1, Map.of("policyId", "POL-704", "changeType", "add-vehicle", "details", "2024 Honda Civic"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("applied: %s%n", workflow.getOutput().get("applied"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
