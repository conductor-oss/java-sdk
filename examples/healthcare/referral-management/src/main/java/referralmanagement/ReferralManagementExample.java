package referralmanagement;

import com.netflix.conductor.client.worker.Worker;
import referralmanagement.workers.*;

import java.util.List;
import java.util.Map;

public class ReferralManagementExample {

    private static final List<Worker> WORKERS = List.of(
            new CreateReferralWorker(),
            new MatchSpecialistWorker(),
            new ScheduleReferralWorker(),
            new TrackReferralWorker(),
            new CloseReferralWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Referral Management Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("ref_create", "ref_match_specialist", "ref_schedule", "ref_track", "ref_close"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("referral_management_workflow", 1, Map.of(
                "referralId", "REF-2024-0445",
                "patientId", "PAT-10234",
                "specialty", "Orthopedics",
                "reason", "Chronic knee pain — evaluate for surgical intervention"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Specialist: " + result.getOutput().get("specialist"));
        System.out.println("Appointment: " + result.getOutput().get("appointmentDate"));
        System.out.println("Outcome: " + result.getOutput().get("outcome"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
