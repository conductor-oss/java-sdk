package backpressure;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import backpressure.workers.BkpMonitorQueueWorker;
import backpressure.workers.BkpHandleOkWorker;
import backpressure.workers.BkpThrottleWorker;
import backpressure.workers.BkpShedLoadWorker;

import java.util.List;
import java.util.Map;

/**
 * Backpressure Demo
 *
 * Run:
 *   java -jar target/backpressure-1.0.0.jar
 */
public class BackpressureExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Backpressure Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "bkp_monitor_queue",
                "bkp_handle_ok",
                "bkp_throttle",
                "bkp_shed_load"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'bkp_backpressure'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new BkpMonitorQueueWorker(),
                new BkpHandleOkWorker(),
                new BkpThrottleWorker(),
                new BkpShedLoadWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("bkp_backpressure", 1,
                Map.of("queueName", "ingest_queue", "thresholdHigh", 500, "thresholdCritical", 1000));
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