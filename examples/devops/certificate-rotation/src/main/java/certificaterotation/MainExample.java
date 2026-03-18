package certificaterotation;

import com.netflix.conductor.client.worker.Worker;
import certificaterotation.workers.DiscoverWorker;
import certificaterotation.workers.GenerateWorker;
import certificaterotation.workers.DeployWorker;
import certificaterotation.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 331: Certificate Rotation — Automated TLS Certificate Management
 *
 * Pattern: discover -> generate -> deploy -> verify
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 331: Certificate Rotation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "cr_discover",
                "cr_generate",
                "cr_deploy",
                "cr_verify"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DiscoverWorker(),
                new GenerateWorker(),
                new DeployWorker(),
                new VerifyWorker()
        );
        helper.startWorkers(workers);


        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = helper.startWorkflow("certificate_rotation_workflow", 1, Map.of(
                "domain", "api.example.com",
                "certType", "RSA-2048"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  discoverResult: " + execution.getOutput().get("discoverResult"));
        System.out.println("  verifyResult: " + execution.getOutput().get("verifyResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
