package identityprovisioning;

import com.netflix.conductor.client.worker.Worker;
import identityprovisioning.workers.CreateIdentityWorker;
import identityprovisioning.workers.AssignRolesWorker;
import identityprovisioning.workers.ProvisionAccessWorker;
import identityprovisioning.workers.VerifySetupWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 358: Identity Provisioning — User Lifecycle Management
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 358: Identity Provisioning ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ip_create_identity",
                "ip_assign_roles",
                "ip_provision_access",
                "ip_verify_setup"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CreateIdentityWorker(),
                new AssignRolesWorker(),
                new ProvisionAccessWorker(),
                new VerifySetupWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("identity_provisioning_workflow", 1, Map.of(
                "userId", "jane.doe",
                "department", "engineering",
                "role", "senior-engineer"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  create_identityResult: " + execution.getOutput().get("create_identityResult"));
        System.out.println("  verify_setupResult: " + execution.getOutput().get("verify_setupResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
