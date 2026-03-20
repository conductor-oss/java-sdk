package billingmedical;

import com.netflix.conductor.client.worker.Worker;
import billingmedical.workers.*;

import java.util.List;
import java.util.Map;

public class BillingMedicalExample {

    private static final List<Worker> WORKERS = List.of(
            new CodeProceduresWorker(),
            new VerifyCoverageWorker(),
            new SubmitClaimWorker(),
            new TrackPaymentWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Medical Billing Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("mbl_code_procedures", "mbl_verify_coverage", "mbl_submit_claim", "mbl_track_payment"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("medical_billing_workflow", 1, Map.of(
                "encounterId", "ENC-2024-0301",
                "patientId", "PAT-10234",
                "providerId", "PROV-5501",
                "procedures", List.of("office_visit", "blood_draw", "cmp")
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        var output = result.getOutput();
        System.out.println("Claim: " + output.get("claimId"));
        System.out.println("Total charge: $" + output.get("totalCharge"));
        System.out.println("Payment status: " + output.get("paymentStatus"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
