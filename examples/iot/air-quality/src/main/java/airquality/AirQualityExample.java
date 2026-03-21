package airquality;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import airquality.workers.CollectReadingsWorker;
import airquality.workers.CheckStandardsWorker;
import airquality.workers.ActionGoodWorker;
import airquality.workers.ActionModerateWorker;
import airquality.workers.ActionPoorWorker;
import java.util.List;
import java.util.Map;
public class AirQualityExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 549: Air Quality ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("aq_collect_readings", "aq_check_standards", "aq_action_good", "aq_action_moderate", "aq_action_poor"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectReadingsWorker(), new CheckStandardsWorker(), new ActionGoodWorker(), new ActionModerateWorker(), new ActionPoorWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("air_quality_demo", 1, Map.of("stationId", "AQ-CLEAN", "region", "suburban"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
