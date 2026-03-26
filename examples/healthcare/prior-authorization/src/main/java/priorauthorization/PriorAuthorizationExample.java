package priorauthorization;

import com.netflix.conductor.client.worker.Worker;
import priorauthorization.workers.*;

import java.util.List;
import java.util.Map;

public class PriorAuthorizationExample {

    private static final List<Worker> WORKERS = List.of(
            new SubmitRequestWorker(),
            new ReviewCriteriaWorker(),
            new ApproveWorker(),
            new DenyWorker(),
            new ManualReviewWorker(),
            new NotifyWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Prior Authorization Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("pa_submit_request", "pa_review_criteria", "pa_approve", "pa_deny", "pa_manual_review", "pa_notify"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("prior_authorization_workflow", 1, Map.of(
                "authId", "PA-2024-0891",
                "patientId", "PAT-10234",
                "procedure", "MRI Lumbar Spine",
                "clinicalReason", "Chronic back pain — medically necessary for surgical planning"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Decision: " + result.getOutput().get("decision"));
        System.out.println("Notified: " + result.getOutput().get("notified"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
