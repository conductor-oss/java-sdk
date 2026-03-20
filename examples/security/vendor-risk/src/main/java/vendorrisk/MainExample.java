package vendorrisk;

import com.netflix.conductor.client.worker.Worker;
import vendorrisk.workers.CollectQuestionnaireWorker;
import vendorrisk.workers.AssessRiskWorker;
import vendorrisk.workers.ReviewSoc2Worker;
import vendorrisk.workers.MakeDecisionWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 362: Vendor Risk — Third-Party Risk Assessment Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 362: Vendor Risk ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "vr_collect_questionnaire",
                "vr_assess_risk",
                "vr_review_soc2",
                "vr_make_decision"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectQuestionnaireWorker(),
                new AssessRiskWorker(),
                new ReviewSoc2Worker(),
                new MakeDecisionWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("vendor_risk_workflow", 1, Map.of(
                "vendorName", "CloudAnalytics Inc",
                "dataAccess", "customer-pii"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  collect_questionnaireResult: " + execution.getOutput().get("collect_questionnaireResult"));
        System.out.println("  make_decisionResult: " + execution.getOutput().get("make_decisionResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
