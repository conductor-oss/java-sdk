package travelapproval;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import travelapproval.workers.*;
import java.util.List; import java.util.Map;
public class TravelApprovalExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Travel Request Approval ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tva_submit","tva_estimate","tva_auto_approve","tva_manager_approve"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SubmitWorker(),new EstimateWorker(),new AutoApproveWorker(),new ManagerApproveWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tva_travel_approval", 1, Map.of("employeeId","EMP-800","destination","New York","purpose","client meeting","estimatedCost",2500));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Request ID: " + wf.getOutput().get("requestId"));
        System.out.println("  Approval type: " + wf.getOutput().get("approvalType"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
