package workerhealthchecks;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import workerhealthchecks.workers.WhcWorker;

import java.util.List;
import java.util.Map;

/**
 * Worker Health Checks — Detect Unhealthy Workers
 *
 * Demonstrates how to monitor worker health using:
 * - Internal counters (pollCount, completedCount) via AtomicInteger
 * - Conductor health monitoring APIs: /tasks/queue/sizes, /tasks/queue/polldata
 *
 * Run:
 *   java -jar target/worker-health-checks-1.0.0.jar
 *   java -jar target/worker-health-checks-1.0.0.jar --workers
 */
public class WorkerHealthChecksExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Worker Health Checks Demo: Detect Unhealthy Workers ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");

        TaskDef whcTask = new TaskDef();
        whcTask.setName("whc_task");
        whcTask.setRetryCount(0);
        whcTask.setTimeoutSeconds(60);
        whcTask.setResponseTimeoutSeconds(30);
        whcTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(whcTask));

        System.out.println("\n  Registered: whc_task");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'worker_health_checks_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        WhcWorker whcWorker = new WhcWorker();
        List<Worker> workers = List.of(whcWorker);
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run workflows and monitor health
        System.out.println("Step 4: Starting 3 workflows to generate health data...\n");

        String[] workflowIds = new String[3];
        for (int i = 0; i < 3; i++) {
            workflowIds[i] = client.startWorkflow("worker_health_checks_demo", 1,
                    Map.of("data", "item-" + i));
            System.out.println("  Started workflow " + (i + 1) + ": " + workflowIds[i]);
        }

        System.out.println();

        // Step 5 — Wait for all workflows
        System.out.println("Step 5: Waiting for workflows to complete...\n");
        boolean allPassed = true;
        for (int i = 0; i < 3; i++) {
            Workflow wf = client.waitForWorkflow(workflowIds[i], "COMPLETED", 30000);
            String status = wf.getStatus().name();
            System.out.println("  Workflow " + (i + 1) + ": " + status + " | Output: " + wf.getOutput());
            if (!"COMPLETED".equals(status)) {
                allPassed = false;
            }
        }

        // Step 6 — Check worker health stats
        System.out.println("\nStep 6: Checking worker health stats...\n");
        System.out.println("  Worker pollCount:      " + whcWorker.getPollCount());
        System.out.println("  Worker completedCount: " + whcWorker.getCompletedCount());
        System.out.println();

        System.out.println("  Health monitoring APIs available:");
        System.out.println("    GET /tasks/queue/sizes       — Check queue backlog (high = workers may be unhealthy)");
        System.out.println("    GET /tasks/queue/polldata    — Check last poll times (stale = worker may be down)");
        System.out.println();

        if (whcWorker.getPollCount() >= 3 && whcWorker.getCompletedCount() >= 3) {
            System.out.println("  Worker health: HEALTHY (poll and completed counts match expected values)");
        } else {
            System.out.println("  Worker health: WARNING (counts lower than expected)");
            allPassed = false;
        }

        client.stopWorkers();

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
