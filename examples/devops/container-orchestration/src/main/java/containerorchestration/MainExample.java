package containerorchestration;

import com.netflix.conductor.client.worker.Worker;
import containerorchestration.workers.BuildWorker;
import containerorchestration.workers.ScanWorker;
import containerorchestration.workers.DeployWorker;
import containerorchestration.workers.HealthCheckWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 333: Container Orchestration — Lifecycle Management for Containers
 *
 * Pattern: build -> scan -> deploy -> healthcheck
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 333: Container Orchestration ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "co_build",
                "co_scan",
                "co_deploy",
                "co_health_check"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new BuildWorker(),
                new ScanWorker(),
                new DeployWorker(),
                new HealthCheckWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("container_orchestration_workflow", 1, Map.of(
                "image", "user-service",
                "tag", "v2.1.0",
                "cluster", "prod-east"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  buildResult: " + execution.getOutput().get("buildResult"));
        System.out.println("  health_checkResult: " + execution.getOutput().get("health_checkResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
