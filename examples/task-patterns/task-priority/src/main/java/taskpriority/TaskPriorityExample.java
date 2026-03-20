package taskpriority;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskpriority.workers.PriProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Priority — Priority levels 0-99 (higher = more important)
 *
 * Demonstrates setting workflow priority in Conductor.
 * Multiple workflows are started with different priorities to show
 * how higher-priority workflows get processed first when workers
 * are constrained.
 *
 * Run:
 *   java -jar target/task-priority-1.0.0.jar
 */
public class TaskPriorityExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Priority Demo: Priority levels 0-99 ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("pri_process"));
        System.out.println("  Registered: pri_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'priority_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PriProcessWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start workflows with different priorities
        System.out.println("Step 4: Starting workflows with different priorities...\n");

        record PriorityRequest(String requestId, int priority) {}

        List<PriorityRequest> requests = List.of(
                new PriorityRequest("req-low", 10),
                new PriorityRequest("req-medium", 50),
                new PriorityRequest("req-critical", 99),
                new PriorityRequest("req-default", 0),
                new PriorityRequest("req-high", 75)
        );

        String[] workflowIds = new String[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            PriorityRequest req = requests.get(i);
            Map<String, Object> input = Map.of(
                    "requestId", req.requestId(),
                    "priority", req.priority()
            );
            workflowIds[i] = client.startWorkflow("priority_demo", 1, input, req.priority());
            System.out.println("  Started: " + req.requestId()
                    + " (priority=" + req.priority() + ") -> " + workflowIds[i]);
        }
        System.out.println();

        // Step 5 — Wait for all workflows to complete
        System.out.println("Step 5: Waiting for all workflows to complete...\n");

        boolean allPassed = true;
        for (int i = 0; i < workflowIds.length; i++) {
            PriorityRequest req = requests.get(i);
            Workflow workflow = client.waitForWorkflow(workflowIds[i], "COMPLETED", 30000);
            String status = workflow.getStatus().name();
            System.out.println("  " + req.requestId() + " (priority=" + req.priority()
                    + "): " + status + " -> " + workflow.getOutput());
            if (!"COMPLETED".equals(status)) {
                allPassed = false;
            }
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
