package infrastructureprovisioning;

import com.netflix.conductor.client.worker.Worker;
import infrastructureprovisioning.workers.*;

import java.util.*;

/**
 * Infrastructure Provisioning — IaC Orchestration
 *
 * Orchestrates infrastructure provisioning: plan, validate, provision,
 * configure, and verify across cloud providers.
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/infrastructure-provisioning-1.0.0.jar
 */
public class InfrastructureProvisioningExample {

    private static final List<String> TASK_NAMES = List.of(
            "ip_plan", "ip_validate", "ip_provision", "ip_configure", "ip_verify"
    );

    private static List<Worker> allWorkers() {
        return List.of(new Plan(), new Validate(), new Provision(), new Configure(), new Verify());
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Infrastructure Provisioning Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(TASK_NAMES);
        client.registerWorkflow("workflow.json");

        List<Worker> workers = allWorkers();
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("environment", "production");
        input.put("region", "us-east-1");
        input.put("resourceType", "kubernetes-cluster");

        String workflowId = client.startWorkflow("infra_provisioning_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId);

        var workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("  Status: " + workflow.getStatus().name());

        client.stopWorkers();
    }
}
