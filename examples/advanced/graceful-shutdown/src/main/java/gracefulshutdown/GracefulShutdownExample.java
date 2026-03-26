package gracefulshutdown;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gracefulshutdown.workers.GshSignalWorker;
import gracefulshutdown.workers.GshDrainTasksWorker;
import gracefulshutdown.workers.GshCompleteInflightWorker;
import gracefulshutdown.workers.GshCheckpointWorker;
import gracefulshutdown.workers.GshStopWorker;

import java.util.List;
import java.util.Map;

/**
 * Graceful Shutdown Demo
 *
 * Run:
 *   java -jar target/gracefulshutdown-1.0.0.jar
 */
public class GracefulShutdownExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Graceful Shutdown Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gsh_signal",
                "gsh_drain_tasks",
                "gsh_complete_inflight",
                "gsh_checkpoint",
                "gsh_stop"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'gsh_graceful_shutdown'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GshSignalWorker(),
                new GshDrainTasksWorker(),
                new GshCompleteInflightWorker(),
                new GshCheckpointWorker(),
                new GshStopWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("gsh_graceful_shutdown", 1,
                Map.of("workerGroup", "order-processors", "drainTimeoutSec", 30));
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