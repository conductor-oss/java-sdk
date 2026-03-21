package secretrotation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import secretrotation.workers.*;
import java.util.List;
import java.util.Map;

public class SecretRotationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 300: Secret Rotation ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sr_generate_secret", "sr_store_secret", "sr_update_services", "sr_verify_rotation"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new GenerateSecretWorker(), new StoreSecretWorker(), new UpdateServicesWorker(), new VerifyRotationWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("secret_rotation_300", 1,
                Map.of("secretName", "db-password", "targetServices", List.of("api-service", "worker-service", "scheduler-service")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
