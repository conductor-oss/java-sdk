package accessreview;

import com.netflix.conductor.client.worker.Worker;
import accessreview.workers.CollectEntitlementsWorker;
import accessreview.workers.IdentifyAnomaliesWorker;
import accessreview.workers.RequestCertificationWorker;
import accessreview.workers.EnforceDecisionsWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 300: Access Review — Automated Access Certification Campaign
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 300: Access Review ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ar_collect_entitlements",
                "ar_identify_anomalies",
                "ar_request_certification",
                "ar_enforce_decisions"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectEntitlementsWorker(),
                new IdentifyAnomaliesWorker(),
                new RequestCertificationWorker(),
                new EnforceDecisionsWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("access_review_workflow", 1, Map.of(
                "department", "engineering",
                "reviewCycle", "Q1-2024"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  collect_entitlementsResult: " + execution.getOutput().get("collect_entitlementsResult"));
        System.out.println("  enforce_decisionsResult: " + execution.getOutput().get("enforce_decisionsResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
