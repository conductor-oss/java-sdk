package changerequest;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import changerequest.workers.*;
import java.util.List; import java.util.Map;
public class ChangeRequestExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 360: Change Request ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("chr_submit","chr_assess_impact","chr_approve","chr_implement","chr_verify"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SubmitWorker(),new AssessImpactWorker(),new ApproveWorker(),new ImplementWorker(),new VerifyWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("change_request_change-request", 1, Map.of("changeId","CR-101","description","Add mobile support to dashboard","requestedBy","Product Manager"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
