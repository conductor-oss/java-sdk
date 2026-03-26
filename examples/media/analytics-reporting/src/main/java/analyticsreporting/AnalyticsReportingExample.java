package analyticsreporting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import analyticsreporting.workers.CollectEventsWorker;
import analyticsreporting.workers.AggregateDataWorker;
import analyticsreporting.workers.ComputeMetricsWorker;
import analyticsreporting.workers.GenerateReportWorker;
import java.util.List;
import java.util.Map;
public class AnalyticsReportingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 526: Analytics Reporting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("anr_collect_events", "anr_aggregate_data", "anr_compute_metrics", "anr_generate_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectEventsWorker(), new AggregateDataWorker(), new ComputeMetricsWorker(), new GenerateReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("analytics_reporting_workflow", 1, Map.of("reportId", "RPT-526-WEEKLY", "dateRange", "{'start': '2026-03-01', 'end': '2026-03-07'}", "dataSources", List.of("web_analytics", "mobile_analytics", "api_logs")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
