package thresholdalerting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import thresholdalerting.workers.*;
import java.util.List;
import java.util.Map;

public class ThresholdAlertingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 423: Threshold Alerting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("th_check_metric", "th_page_oncall", "th_send_warning", "th_log_ok"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckMetricWorker(), new PageOncallWorker(), new SendWarningWorker(), new LogOkWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("threshold_alerting_423", 1, Map.of("metricName","error_rate_percent","currentValue",12.5,"warningThreshold",5,"criticalThreshold",10));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
