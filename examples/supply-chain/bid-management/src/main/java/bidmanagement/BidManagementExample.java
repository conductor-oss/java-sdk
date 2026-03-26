package bidmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bidmanagement.workers.*;
import java.util.*;

public class BidManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 663: Bid Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("bid_create","bid_distribute","bid_collect","bid_evaluate","bid_award"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CreateWorker(), new DistributeWorker(), new CollectWorker(), new EvaluateWorker(), new AwardWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("bid_management", 1,
                Map.of("projectName","Warehouse Expansion","budget",100000,"vendors",List.of("Alpha Corp","Beta Ltd","Gamma Inc")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
