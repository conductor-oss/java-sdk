package watermanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import watermanagement.workers.MonitorLevelsWorker;
import watermanagement.workers.AnalyzeQualityWorker;
import watermanagement.workers.DetectLeaksWorker;
import watermanagement.workers.AlertWorker;
import java.util.List;
import java.util.Map;
public class WaterManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 548: Water Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("wtr_monitor_levels", "wtr_analyze_quality", "wtr_detect_leaks", "wtr_alert"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorLevelsWorker(), new AnalyzeQualityWorker(), new DetectLeaksWorker(), new AlertWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("water_management_demo", 1, Map.of("zoneId", "DIST-ZONE-4", "sensorGroup", "SG-12"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
