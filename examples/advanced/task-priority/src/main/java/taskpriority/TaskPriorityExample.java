package taskpriority;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskpriority.workers.TprClassifyPriorityWorker;
import taskpriority.workers.TprRouteHighWorker;
import taskpriority.workers.TprRouteMediumWorker;
import taskpriority.workers.TprRouteLowWorker;

import java.util.List;
import java.util.Map;

/**
 * Task Priority Demo
 *
 * Run:
 *   java -jar target/taskpriority-1.0.0.jar
 */
public class TaskPriorityExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Task Priority Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tpr_classify_priority",
                "tpr_route_high",
                "tpr_route_medium",
                "tpr_route_low"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'tpr_task_priority'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new TprClassifyPriorityWorker(),
                new TprRouteHighWorker(),
                new TprRouteMediumWorker(),
                new TprRouteLowWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("tpr_task_priority", 1,
                Map.of("taskId", "TASK-9001", "urgency", "high", "impact", "high"));
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