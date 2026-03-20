package environmentalmonitoring;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import environmentalmonitoring.workers.CollectDataWorker;
import environmentalmonitoring.workers.CheckThresholdsWorker;
import environmentalmonitoring.workers.TriggerAlertWorker;
import environmentalmonitoring.workers.GenerateReportWorker;
import java.util.List;
import java.util.Map;
public class EnvironmentalMonitoringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 539: Environmental Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("env_collect_data", "env_check_thresholds", "env_trigger_alert", "env_generate_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new CheckThresholdsWorker(), new TriggerAlertWorker(), new GenerateReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("environmental_monitoring_workflow", 1, Map.of("stationId", "ENV-STN-539-NORTH", "region", "Industrial Zone A", "monitoringType", "air_quality"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
