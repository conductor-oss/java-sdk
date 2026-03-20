package costmonitoring;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import costmonitoring.workers.*;
import java.util.List;
import java.util.Map;

public class CostMonitoringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 419: Cost Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cos_collect_billing", "cos_analyze_trends", "cos_alert_anomalies"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectBillingWorker(), new AnalyzeTrendsWorker(), new CosAlertWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cost_monitoring_419", 1, Map.of("accountId","acct-prod-001","billingPeriod","2026-03","budgetLimit",15000));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
