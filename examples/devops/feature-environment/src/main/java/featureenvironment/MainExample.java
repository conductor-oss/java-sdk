package featureenvironment;

import com.netflix.conductor.client.worker.Worker;
import featureenvironment.workers.ProvisionWorker;
import featureenvironment.workers.DeployBranchWorker;
import featureenvironment.workers.ConfigureDnsWorker;
import featureenvironment.workers.NotifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 360: Feature Environment — On-Demand Preview Environment Provisioning
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 360: Feature Environment ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "fe_provision",
                "fe_deploy_branch",
                "fe_configure_dns",
                "fe_notify"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ProvisionWorker(),
                new DeployBranchWorker(),
                new ConfigureDnsWorker(),
                new NotifyWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("feature_environment_workflow", 1, Map.of(
                "branch", "feature-auth-v2",
                "repository", "main-app"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  provisionResult: " + execution.getOutput().get("provisionResult"));
        System.out.println("  notifyResult: " + execution.getOutput().get("notifyResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
