package useranalytics;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import useranalytics.workers.*;
import java.util.List;
import java.util.Map;

public class UserAnalyticsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 617: User Analytics ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("uan_collect_events", "uan_aggregate", "uan_compute_metrics", "uan_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectEventsWorker(), new AggregateWorker(), new ComputeMetricsWorker(), new AnalyticsReportWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("uan_user_analytics", 1,
                Map.of("dateRange", Map.of("start", "2024-10-01", "end", "2024-12-31"), "segments", List.of("free", "pro", "enterprise")));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
