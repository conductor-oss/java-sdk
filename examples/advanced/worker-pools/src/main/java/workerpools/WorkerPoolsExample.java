package workerpools;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workerpools.workers.WplCategorizeTaskWorker;
import workerpools.workers.WplAssignPoolWorker;
import workerpools.workers.WplExecuteTaskWorker;
import workerpools.workers.WplReturnToPoolWorker;

import java.util.List;
import java.util.Map;

/**
 * Worker Pools Demo
 *
 * Run:
 *   java -jar target/workerpools-1.0.0.jar
 */
public class WorkerPoolsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Worker Pools Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wpl_categorize_task",
                "wpl_assign_pool",
                "wpl_execute_task",
                "wpl_return_to_pool"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'wpl_worker_pools'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WplCategorizeTaskWorker(),
                new WplAssignPoolWorker(),
                new WplExecuteTaskWorker(),
                new WplReturnToPoolWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wpl_worker_pools", 1,
                Map.of("taskPayload", "process_batch", "taskCategory", "compute"));
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