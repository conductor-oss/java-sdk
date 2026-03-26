package telemedicine;

import com.netflix.conductor.client.worker.Worker;
import telemedicine.workers.*;

import java.util.List;
import java.util.Map;

public class TelemedicineExample {

    private static final List<Worker> WORKERS = List.of(
            new ScheduleWorker(),
            new ConnectWorker(),
            new ConsultWorker(),
            new PrescribeWorker(),
            new FollowUpWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Telemedicine Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("tlm_schedule", "tlm_connect", "tlm_consult", "tlm_prescribe", "tlm_followup"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("telemedicine_workflow", 1, Map.of(
                "visitId", "TLM-2024-0301",
                "patientId", "PAT-10234",
                "providerId", "DR-CHEN-001",
                "reason", "Persistent cough and congestion"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Diagnosis: " + result.getOutput().get("diagnosis"));
        System.out.println("Prescription: " + result.getOutput().get("prescription"));
        System.out.println("Follow-up: " + result.getOutput().get("followUpDate"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
