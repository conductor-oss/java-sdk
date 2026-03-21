package mentalhealth;

import com.netflix.conductor.client.worker.Worker;
import mentalhealth.workers.*;

import java.util.List;
import java.util.Map;

public class MentalHealthExample {

    private static final List<Worker> WORKERS = List.of(
            new IntakeWorker(),
            new AssessWorker(),
            new TreatmentPlanWorker(),
            new TrackProgressWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Mental Health Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("mh_intake", "mh_assess", "mh_treatment_plan", "mh_track_progress"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("mental_health_workflow", 1, Map.of(
                "patientId", "PAT-20456",
                "referralReason", "Depressed mood and insomnia for 6 weeks",
                "provider", "Dr. Thompson, Psychiatry"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Diagnosis: " + result.getOutput().get("diagnosis"));
        System.out.println("Treatment: " + result.getOutput().get("treatmentPlan"));
        System.out.println("Tracking active: " + result.getOutput().get("trackingActive"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
