package predictivemonitoring;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import predictivemonitoring.workers.*;
import java.util.List;
import java.util.Map;

public class PredictiveMonitoringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 424: Predictive Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pdm_collect_history", "pdm_train_model", "pdm_predict", "pdm_alert"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectHistoryWorker(), new TrainModelWorker(), new PredictWorker(), new PdmAlertWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("predictive_monitoring_424", 1, Map.of("metricName","cpu_usage_percent","historyDays",30,"forecastHours",12));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
