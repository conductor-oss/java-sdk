package workflowtimeout;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowtimeout.workers.FastWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Timeout — Set maximum execution time for workflows.
 *
 * Demonstrates two workflows with different timeout settings:
 * - wf_timeout_demo: 30-second timeout (short)
 * - wf_timeout_long: 3600-second timeout (1 hour)
 *
 * Both use the same fast-completing worker (wft_fast) to show
 * that timeout is a workflow-level setting, not a task-level one.
 *
 * Run:
 *   java -jar target/workflow-timeout-1.0.0.jar
 */
public class WorkflowTimeoutExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Timeout: Short (30s) vs Long (3600s) ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("wft_fast"));
        System.out.println("  Registered: wft_fast\n");

        // Step 2 — Register workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("workflow.json");
        client.registerWorkflow("workflow-long.json");
        System.out.println("  Registered: wf_timeout_demo (30s timeout)");
        System.out.println("  Registered: wf_timeout_long (3600s timeout)\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new FastWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the short-timeout workflow
        System.out.println("Step 4: Starting wf_timeout_demo (30s timeout)...");
        String demoId = client.startWorkflow("wf_timeout_demo", 1,
                Map.of("mode", "quick"));
        System.out.println("  Workflow ID: " + demoId + "\n");

        // Step 5 — Wait for short-timeout completion
        System.out.println("Step 5: Waiting for wf_timeout_demo...");
        Workflow demoWorkflow = client.waitForWorkflow(demoId, "COMPLETED", 30000);
        String demoStatus = demoWorkflow.getStatus().name();
        System.out.println("  Status: " + demoStatus);
        System.out.println("  Output: " + demoWorkflow.getOutput() + "\n");

        // Step 6 — Start the long-timeout workflow
        System.out.println("Step 6: Starting wf_timeout_long (3600s timeout)...");
        String longId = client.startWorkflow("wf_timeout_long", 1,
                Map.of("mode", "extended"));
        System.out.println("  Workflow ID: " + longId + "\n");

        // Step 7 — Wait for long-timeout completion
        System.out.println("Step 7: Waiting for wf_timeout_long...");
        Workflow longWorkflow = client.waitForWorkflow(longId, "COMPLETED", 30000);
        String longStatus = longWorkflow.getStatus().name();
        System.out.println("  Status: " + longStatus);
        System.out.println("  Output: " + longWorkflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(demoStatus) && "COMPLETED".equals(longStatus)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
