package reimbursement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import reimbursement.workers.*;
import java.util.List; import java.util.Map;
public class ReimbursementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example reimbursement: Reimbursement ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rmb_submit","rmb_validate","rmb_approve","rmb_process","rmb_notify"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SubmitWorker(),new ValidateWorker(),new ApproveWorker(),new ProcessWorker(),new NotifyWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rmb_reimbursement", 1, Map.of("employeeId","EMP-1100","amount",875,"category","travel","receiptCount",6));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Claim ID: " + wf.getOutput().get("claimId"));
        System.out.println("  Payment ID: " + wf.getOutput().get("paymentId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
