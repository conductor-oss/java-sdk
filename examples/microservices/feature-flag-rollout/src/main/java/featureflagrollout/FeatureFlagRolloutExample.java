package featureflagrollout;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import featureflagrollout.workers.*;
import java.util.List;
import java.util.Map;

public class FeatureFlagRolloutExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Feature Flag Rollout Demo ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ff_create_flag", "ff_enable_segment", "ff_monitor_impact", "ff_full_rollout"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateFlagWorker(), new EnableSegmentWorker(), new MonitorImpactWorker(), new FullRolloutWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("feature_flag_rollout", 1, Map.of("flagName", "new-checkout-flow", "targetSegment", "beta-users", "rolloutPercentage", 10));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        client.stopWorkers();
    }
}
