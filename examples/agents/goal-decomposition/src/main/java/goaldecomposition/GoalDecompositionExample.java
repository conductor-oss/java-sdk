package goaldecomposition;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import goaldecomposition.workers.DecomposeGoalWorker;
import goaldecomposition.workers.Subgoal1Worker;
import goaldecomposition.workers.Subgoal2Worker;
import goaldecomposition.workers.Subgoal3Worker;
import goaldecomposition.workers.AggregateWorker;

import java.util.List;
import java.util.Map;

/**
 * Goal Decomposition Demo
 *
 * Demonstrates a FORK/JOIN pattern that decomposes a high-level goal into
 * three subgoals, executes them in parallel, then aggregates the results:
 *   gd_decompose_goal -> FORK(gd_subgoal_1, gd_subgoal_2, gd_subgoal_3) -> JOIN -> gd_aggregate
 *
 * Run:
 *   java -jar target/goal-decomposition-1.0.0.jar
 */
public class GoalDecompositionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Goal Decomposition Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gd_decompose_goal", "gd_subgoal_1",
                "gd_subgoal_2", "gd_subgoal_3", "gd_aggregate"));
        System.out.println("  Registered: gd_decompose_goal, gd_subgoal_1, gd_subgoal_2, gd_subgoal_3, gd_aggregate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'goal_decomposition'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DecomposeGoalWorker(),
                new Subgoal1Worker(),
                new Subgoal2Worker(),
                new Subgoal3Worker(),
                new AggregateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("goal_decomposition", 1,
                Map.of("goal", "Improve application performance by 3x"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
