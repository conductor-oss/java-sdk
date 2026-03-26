package milestonetracking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import milestonetracking.workers.*;
import java.util.List; import java.util.Map;
public class MilestoneTrackingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example milestone-tracking: Milestone Tracking ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mst_check_progress","mst_evaluate","mst_on_track","mst_at_risk","mst_delayed","mst_act"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckProgressWorker(),new EvaluateWorker(),new OnTrackWorker(),new AtRiskWorker(),new DelayedWorker(),new ActWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("milestone_tracking_724", 1, Map.of("milestoneId","MS-Q1-2026","projectName","Project Alpha"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
