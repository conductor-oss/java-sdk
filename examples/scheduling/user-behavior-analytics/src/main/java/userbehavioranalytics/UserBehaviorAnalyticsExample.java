package userbehavioranalytics;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import userbehavioranalytics.workers.*;
import java.util.List;
import java.util.Map;

public class UserBehaviorAnalyticsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 429: User Behavior Analytics ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("uba_collect_events", "uba_sessionize", "uba_analyze_patterns", "uba_flag_anomalies"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectEventsWorker(), new SessionizeWorker(), new AnalyzePatternsWorker(), new FlagAnomaliesWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("user_behavior_analytics_429", 1, Map.of("userId","user-8842","timeRange","last-24h","riskThreshold",70));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
