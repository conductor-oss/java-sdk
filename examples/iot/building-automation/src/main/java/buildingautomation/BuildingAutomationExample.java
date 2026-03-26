package buildingautomation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import buildingautomation.workers.MonitorSystemsWorker;
import buildingautomation.workers.OptimizeWorker;
import buildingautomation.workers.ScheduleWorker;
import buildingautomation.workers.AdjustWorker;
import java.util.List;
import java.util.Map;
public class BuildingAutomationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 547: Building Automation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("bld_monitor_systems", "bld_optimize", "bld_schedule", "bld_adjust"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorSystemsWorker(), new OptimizeWorker(), new ScheduleWorker(), new AdjustWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("building_automation_demo", 1, Map.of("buildingId", "HQ-TOWER", "floor", "3"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
