package rfpautomation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import rfpautomation.workers.*;
import java.util.*;
public class RfpAutomationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 664: RFP Automation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rfp_create","rfp_distribute","rfp_collect","rfp_evaluate","rfp_select"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CreateWorker(), new DistributeWorker(), new CollectWorker(), new EvaluateWorker(), new SelectWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rfp_automation", 1,
                Map.of("projectTitle","Cloud Infrastructure Migration","requirements",List.of("scalability","security","24/7 support"),"deadline","2024-04-30"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
