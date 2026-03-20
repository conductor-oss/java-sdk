package gameanalytics;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gameanalytics.workers.*;
import java.util.List;
import java.util.Map;
/** Example 747: Game Analytics — Collect Events, Process, Aggregate, Compute KPIs, Report */
public class GameAnalyticsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 747: Game Analytics ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("gan_collect_events", "gan_process", "gan_aggregate", "gan_compute_kpis", "gan_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectEventsWorker(), new ProcessWorker(), new AggregateWorker(), new ComputeKpisWorker(), new ReportWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("game_analytics_747", 1, Map.of("gameId", "GAME-01", "dateRange", "2026-03-01 to 2026-03-07"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
