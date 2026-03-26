package featureflags;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import featureflags.workers.*;
import java.util.List;
import java.util.Map;

public class FeatureFlagsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 298: Feature Flags ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ff_check_flag", "ff_new_feature", "ff_legacy_path", "ff_default_path", "ff_log_usage"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CheckFlagWorker(), new NewFeatureWorker(), new LegacyPathWorker(), new DefaultPathWorker(), new LogUsageWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("--- Enabled Flag ---");
        String wf1 = client.startWorkflow("feature_flags_298", 1, Map.of("userId", "user-100", "featureName", "new-checkout-ui"));
        Workflow exec1 = client.waitForWorkflow(wf1, "COMPLETED", 60000);
        System.out.println("  Status: " + exec1.getStatus().name() + ", Flag: " + exec1.getOutput().get("flagStatus"));

        System.out.println("--- Disabled Flag ---");
        String wf2 = client.startWorkflow("feature_flags_298", 1, Map.of("userId", "user-200", "featureName", "dark-mode"));
        Workflow exec2 = client.waitForWorkflow(wf2, "COMPLETED", 60000);
        System.out.println("  Status: " + exec2.getStatus().name() + ", Flag: " + exec2.getOutput().get("flagStatus"));

        client.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
