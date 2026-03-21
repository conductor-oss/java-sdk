package liveops;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import liveops.workers.*;
import java.util.List;
import java.util.Map;
/** Example 748: Live Ops — Schedule Event, Configure, Deploy, Monitor, Close */
public class LiveOpsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 748: Live Ops ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("lop_schedule_event", "lop_configure", "lop_deploy", "lop_monitor", "lop_close"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ScheduleEventWorker(), new ConfigureWorker(), new DeployWorker(), new MonitorWorker(), new CloseWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("live_ops_748", 1, Map.of("eventName", "Spring Festival", "startDate", "2026-03-10", "endDate", "2026-03-13"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
