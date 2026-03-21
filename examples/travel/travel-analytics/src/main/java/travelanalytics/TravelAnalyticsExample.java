package travelanalytics;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import travelanalytics.workers.*;
import java.util.List; import java.util.Map;
public class TravelAnalyticsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 550: Travel Analytics ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tan_collect","tan_aggregate","tan_analyze","tan_report"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectWorker(),new AggregateWorker(),new AnalyzeWorker(),new ReportWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tan_travel_analytics", 1, Map.of("period","2024-Q1","department","Engineering"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Total spend: " + wf.getOutput().get("totalSpend"));
        System.out.println("  Report ID: " + wf.getOutput().get("reportId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
