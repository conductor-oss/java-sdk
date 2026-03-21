package riskmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import riskmanagement.workers.*;
import java.util.List; import java.util.Map;
public class RiskManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example risk-management: Risk Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rkm_identify","rkm_assess","rkm_high","rkm_medium","rkm_low","rkm_mitigate"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new IdentifyWorker(),new AssessWorker(),new HighWorker(),new MediumWorker(),new LowWorker(),new MitigateWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("risk_management_726", 1, Map.of("projectId","PROJ-42","riskDescription","Key engineer may leave mid-project"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
