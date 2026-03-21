package uptimemonitoring;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import uptimemonitoring.workers.*;
import java.util.List;
import java.util.Map;

public class UptimeMonitoringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 420: Uptime Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("um_check_endpoint", "um_log_result", "um_calculate_sla", "um_report"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckEndpointWorker(), new LogResultWorker(), new CalculateSlaWorker(), new UmReportWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("uptime_monitoring_420", 1, Map.of("endpoint","https://api.example.com/health","expectedStatus",200,"slaTarget",99.9));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
