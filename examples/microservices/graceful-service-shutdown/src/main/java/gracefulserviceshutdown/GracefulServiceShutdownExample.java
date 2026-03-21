package gracefulserviceshutdown;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gracefulserviceshutdown.workers.StopAcceptingWorker;
import gracefulserviceshutdown.workers.DrainTasksWorker;
import gracefulserviceshutdown.workers.CheckpointWorker;
import gracefulserviceshutdown.workers.DeregisterWorker;

import java.util.List;
import java.util.Map;

/**
 * Graceful Service Shutdown Demo
 *
 * Orchestrates graceful shutdown: stop accepting -> drain tasks -> checkpoint -> deregister.
 *
 * Run:
 *   java -jar target/graceful-service-shutdown-1.0.0.jar
 */
public class GracefulServiceShutdownExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Graceful Service Shutdown Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gs_stop_accepting", "gs_drain_tasks",
                "gs_checkpoint", "gs_deregister"));
        System.out.println("  Registered: gs_stop_accepting, gs_drain_tasks, gs_checkpoint, gs_deregister\n");

        System.out.println("Step 2: Registering workflow 'graceful_shutdown_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new StopAcceptingWorker(),
                new DrainTasksWorker(),
                new CheckpointWorker(),
                new DeregisterWorker()
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
        String workflowId = client.startWorkflow("graceful_shutdown_workflow", 1,
                Map.of("serviceName", "order-service",
                        "instanceId", "order-svc-pod-3"));
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
