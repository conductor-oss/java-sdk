package monitoringai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import monitoringai.workers.*;
import java.util.List;
import java.util.Map;
public class MonitoringAiExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 649: Monitoring AI ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mai_collect_metrics", "mai_detect_anomalies", "mai_diagnose", "mai_recommend"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectMetricsWorker(), new DetectAnomaliesWorker(), new DiagnoseWorker(), new RecommendWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("mai_monitoring_ai", 1,
                Map.of("serviceName", "order-service", "timeWindow", "1h"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
