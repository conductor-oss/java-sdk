package travelpolicy;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import travelpolicy.workers.*;
import java.util.List; import java.util.Map;
public class TravelPolicyExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 537: Travel Policy ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tpl_check","tpl_compliant","tpl_exception","tpl_process"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckWorker(),new CompliantWorker(),new ExceptionWorker(),new ProcessWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tpl_travel_policy", 1, Map.of("employeeId","EMP-1000","bookingType","hotel","amount",500,"policyTier","standard"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Compliance: " + wf.getOutput().get("complianceResult"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
