package usageanalytics;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import usageanalytics.workers.CollectCdrsWorker;
import usageanalytics.workers.ProcessWorker;
import usageanalytics.workers.AggregateWorker;
import usageanalytics.workers.ReportWorker;
import usageanalytics.workers.AlertWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 816: Usage Analytics
 */
public class UsageAnalyticsExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 816: Usage Analytics ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("uag_collect_cdrs", "uag_process", "uag_aggregate", "uag_report", "uag_alert"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectCdrsWorker(), new ProcessWorker(), new AggregateWorker(), new ReportWorker(), new AlertWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("uag_usage_analytics", 1, Map.of("region", "WEST-COAST", "period", "2024-03"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("totalCDRs: %s%n", workflow.getOutput().get("totalCDRs"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
