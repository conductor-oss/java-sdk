package apikeyrotation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apikeyrotation.workers.*;

import java.util.List;
import java.util.Map;

/**
 * API Key Rotation Demo
 *
 * Automated API key lifecycle management:
 *   akr_generate_new -> akr_dual_active -> akr_migrate_consumers -> akr_revoke_old
 */
public class ApiKeyRotationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== API Key Rotation Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "akr_generate_new", "akr_dual_active",
                "akr_migrate_consumers", "akr_revoke_old"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new GenerateNewWorker(), new DualActiveWorker(),
                new MigrateConsumersWorker(), new RevokeOldWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("api_key_rotation_workflow", 1,
                Map.of("service", "payment-gateway", "keyId", "KEY-OLD-001"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
