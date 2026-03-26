package containerorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import containerorchestration.workers.CtrBuildWorker;
import containerorchestration.workers.CtrDeployWorker;
import containerorchestration.workers.CtrScaleWorker;
import containerorchestration.workers.CtrMonitorWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 562: Container Orchestration
 *
 * Build a container image, deploy it, configure auto-scaling,
 * and set up monitoring.
 *
 * Run:
 *   java -jar target/container-orchestration-1.0.0.jar
 */
public class ContainerOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 562: Container Orchestration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ctr_build", "ctr_deploy", "ctr_scale", "ctr_monitor"));
        System.out.println("  Registered: ctr_build, ctr_deploy, ctr_scale, ctr_monitor\n");

        System.out.println("Step 2: Registering workflow 'container_orchestration_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CtrBuildWorker(),
                new CtrDeployWorker(),
                new CtrScaleWorker(),
                new CtrMonitorWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("container_orchestration_demo", 1,
                Map.of("serviceName", "payment-api", "imageTag", "v2.3.1", "replicas", 3));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
