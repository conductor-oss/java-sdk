package clinicaldecision;

import com.netflix.conductor.client.worker.Worker;
import clinicaldecision.workers.*;

import java.util.List;
import java.util.Map;

public class ClinicalDecisionExample {

    private static final List<Worker> WORKERS = List.of(
            new GatherDataWorker(),
            new ApplyGuidelinesWorker(),
            new ScoreRiskWorker(),
            new RecommendWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Clinical Decision Support Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("cds_gather_data", "cds_apply_guidelines", "cds_score_risk", "cds_recommend"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("clinical_decision_workflow", 1, Map.of(
                "patientId", "PAT-30567",
                "condition", "cardiovascular_risk",
                "clinicalContext", "annual_wellness_visit"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Risk score: " + result.getOutput().get("riskScore") + "%");
        System.out.println("Risk category: " + result.getOutput().get("riskCategory"));
        System.out.println("Recommendations: " + result.getOutput().get("recommendations"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
