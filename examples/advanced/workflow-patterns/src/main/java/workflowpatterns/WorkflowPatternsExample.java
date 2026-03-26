package workflowpatterns;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowpatterns.workers.WpChainStepWorker;
import workflowpatterns.workers.WpSplitAWorker;
import workflowpatterns.workers.WpSplitBWorker;
import workflowpatterns.workers.WpMergeResultsWorker;
import workflowpatterns.workers.WpLoopIterationWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Patterns Demo
 *
 * Demonstrates chain, split (FORK), merge (JOIN), and loop (DO_WHILE) patterns.
 *
 * Run:
 *   java -jar target/workflow-patterns-1.0.0.jar
 */
public class WorkflowPatternsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Patterns Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wp_chain_step", "wp_split_a", "wp_split_b",
                "wp_merge_results", "wp_loop_iteration"));
        System.out.println("  Registered: wp_chain_step, wp_split_a, wp_split_b, wp_merge_results, wp_loop_iteration\n");

        System.out.println("Step 2: Registering workflow 'workflow_patterns_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WpChainStepWorker(),
                new WpSplitAWorker(),
                new WpSplitBWorker(),
                new WpMergeResultsWorker(),
                new WpLoopIterationWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("workflow_patterns_demo", 1,
                Map.of("inputData", "sample_payload", "iterations", 3));
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
