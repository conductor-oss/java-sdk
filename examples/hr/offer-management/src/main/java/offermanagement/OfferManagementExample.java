package offermanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import offermanagement.workers.*;
import java.util.List; import java.util.Map;
public class OfferManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 604: Offer Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ofm_generate","ofm_approve","ofm_send","ofm_accept","ofm_decline"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new GenerateWorker(),new ApproveWorker(),new SendWorker(),new AcceptWorker(),new DeclineWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("ofm_offer_management", 1,
                Map.of("candidateName","Alex Rivera","position","Senior Engineer","salary",155000,"response","accept"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Offer ID: " + wf.getOutput().get("offerId"));
        System.out.println("  Response: " + wf.getOutput().get("response"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
