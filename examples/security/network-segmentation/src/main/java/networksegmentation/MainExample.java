package networksegmentation;

import com.netflix.conductor.client.worker.Worker;
import networksegmentation.workers.DefineZonesWorker;
import networksegmentation.workers.ConfigureRulesWorker;
import networksegmentation.workers.ApplyPoliciesWorker;
import networksegmentation.workers.VerifyIsolationWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 600: Network Segmentation — Automated Network Security Zone Management
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 600: Network Segmentation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ns_define_zones",
                "ns_configure_rules",
                "ns_apply_policies",
                "ns_verify_isolation"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DefineZonesWorker(),
                new ConfigureRulesWorker(),
                new ApplyPoliciesWorker(),
                new VerifyIsolationWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("network_segmentation_workflow", 1, Map.of(
                "environment", "production",
                "segmentationType", "micro-segmentation"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  define_zonesResult: " + execution.getOutput().get("define_zonesResult"));
        System.out.println("  verify_isolationResult: " + execution.getOutput().get("verify_isolationResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
