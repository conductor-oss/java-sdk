package insuranceclaims;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import insuranceclaims.workers.*;
import java.util.List;
import java.util.Map;
public class InsuranceClaimsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 475: Insurance Claims ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("clm_submit", "clm_verify", "clm_adjudicate", "clm_pay", "clm_close"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SubmitClaimWorker(), new VerifyClaimWorker(), new AdjudicateClaimWorker(), new PayClaimWorker(), new CloseClaimWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("insurance_claims_workflow", 1, Map.of("claimId", "CLM-2024-0891", "patientId", "PAT-10234", "providerId", "PROV-5501", "amount", 1250.00, "procedureCode", "99213"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
