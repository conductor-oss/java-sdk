package capacitymonitoring;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import capacitymonitoring.workers.*;
import java.util.List;
import java.util.Map;
public class CapacityMonitoringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 418: Capacity Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cap_measure_resources","cap_forecast","cap_alert"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new MeasureResourcesWorker(), new ForecastWorker(), new CapAlertWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("capacity_monitoring_418", 1, Map.of("cluster","prod-us-east","forecastDays",30));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
