package alertingpipeline;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import alertingpipeline.workers.*;
import java.util.List;
import java.util.Map;

public class AlertingPipelineExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 413: Alerting Pipeline ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("alt_detect_anomaly","alt_evaluate_rules","alt_send_alert","alt_suppress_alert"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DetectAnomalyWorker(), new EvaluateRulesWorker(), new SendAlertWorker(), new SuppressAlertWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("alerting_pipeline_413", 1, Map.of("metricName","cpu_usage_percent","currentValue",95,"threshold",80));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
