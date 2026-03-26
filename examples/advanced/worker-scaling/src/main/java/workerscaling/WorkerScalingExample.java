package workerscaling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workerscaling.workers.WksMonitorQueueWorker;
import workerscaling.workers.WksCalculateNeededWorker;
import workerscaling.workers.WksScaleWorkersWorker;
import workerscaling.workers.WksVerifyScalingWorker;

import java.util.List;
import java.util.Map;

/**
 * Worker Scaling Demo
 *
 * Run:
 *   java -jar target/workerscaling-1.0.0.jar
 */
public class WorkerScalingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Worker Scaling Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wks_monitor_queue",
                "wks_calculate_needed",
                "wks_scale_workers",
                "wks_verify_scaling"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'wks_worker_scaling'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WksMonitorQueueWorker(),
                new WksCalculateNeededWorker(),
                new WksScaleWorkersWorker(),
                new WksVerifyScalingWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wks_worker_scaling", 1,
                Map.of("queueName", "order_processing_queue", "currentWorkers", 5, "targetLatencyMs", 5000));
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