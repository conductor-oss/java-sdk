package treeofthought;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import treeofthought.workers.DefineProblemWorker;
import treeofthought.workers.PathAWorker;
import treeofthought.workers.PathBWorker;
import treeofthought.workers.PathCWorker;
import treeofthought.workers.EvaluatePathsWorker;
import treeofthought.workers.SelectBestWorker;

import java.util.List;
import java.util.Map;

/**
 * Tree of Thought Demo
 *
 * Demonstrates parallel exploration of multiple reasoning paths:
 * define problem, fork into three approaches (analytical, creative, empirical),
 * join, evaluate all paths, and select the best solution.
 *   tt_define_problem -> FORK(tt_path_a, tt_path_b, tt_path_c) -> JOIN -> tt_evaluate_paths -> tt_select_best
 *
 * Run:
 *   java -jar target/tree-of-thought-1.0.0.jar
 */
public class TreeOfThoughtExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tree of Thought Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tt_define_problem", "tt_path_a", "tt_path_b",
                "tt_path_c", "tt_evaluate_paths", "tt_select_best"));
        System.out.println("  Registered: tt_define_problem, tt_path_a, tt_path_b, tt_path_c, tt_evaluate_paths, tt_select_best\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tree_of_thought'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DefineProblemWorker(),
                new PathAWorker(),
                new PathBWorker(),
                new PathCWorker(),
                new EvaluatePathsWorker(),
                new SelectBestWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("tree_of_thought", 1,
                Map.of("problem", "Design a highly available system that handles 10x traffic spikes"));
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
