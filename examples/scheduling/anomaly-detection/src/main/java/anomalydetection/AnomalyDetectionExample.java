package anomalydetection;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import anomalydetection.workers.*;
import java.util.List;
import java.util.Map;

public class AnomalyDetectionExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 414: Anomaly Detection ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("anom_collect_data","anom_compute_baseline","anom_detect","anom_classify","anom_alert"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectDataWorker(), new ComputeBaselineWorker(), new DetectWorker(), new ClassifyWorker(), new AlertWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("anomaly_detection_414", 1, Map.of("metricName","request_latency_ms","lookbackHours",24,"sensitivity","high"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
