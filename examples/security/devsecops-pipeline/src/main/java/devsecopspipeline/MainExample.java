package devsecopspipeline;

import com.netflix.conductor.client.worker.Worker;
import devsecopspipeline.workers.SastScanWorker;
import devsecopspipeline.workers.ScaScanWorker;
import devsecopspipeline.workers.ContainerScanWorker;
import devsecopspipeline.workers.SecurityGateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 356: DevSecOps Pipeline — Security-Integrated CI/CD Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 356: DevSecOps Pipeline ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "dso_sast_scan",
                "dso_sca_scan",
                "dso_container_scan",
                "dso_security_gate"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new SastScanWorker(),
                new ScaScanWorker(),
                new ContainerScanWorker(),
                new SecurityGateWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("devsecops_pipeline_workflow", 1, Map.of(
                "repository", "payment-service",
                "commitSha", "abc123def"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  sast_scanResult: " + execution.getOutput().get("sast_scanResult"));
        System.out.println("  security_gateResult: " + execution.getOutput().get("security_gateResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
