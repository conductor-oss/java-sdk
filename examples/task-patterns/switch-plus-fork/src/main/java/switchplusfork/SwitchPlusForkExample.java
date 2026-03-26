package switchplusfork;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import switchplusfork.workers.*;

import java.util.List;
import java.util.Map;

/**
 * SWITCH + FORK Demo — Conditional Parallel Execution
 *
 * Demonstrates combining SWITCH and FORK_JOIN patterns: a SWITCH task
 * routes by type — "batch" triggers a FORK with two parallel lanes
 * (process_a, process_b) + JOIN, while any other type runs a single
 * process. After the switch, sf_finalize runs for all paths.
 *
 * Run:
 *   java -jar target/switch-plus-fork-1.0.0.jar
 *   java -jar target/switch-plus-fork-1.0.0.jar --workers
 */
public class SwitchPlusForkExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SWITCH + FORK Demo: Conditional Parallel Execution ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sf_process_a", "sf_process_b", "sf_single_process", "sf_finalize"
        ));
        System.out.println("  Registered: sf_process_a, sf_process_b, sf_single_process, sf_finalize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'switch_fork_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessAWorker(),
                new ProcessBWorker(),
                new SingleProcessWorker(),
                new FinalizeWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in --workers mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: Batch type -> FORK(process_a, process_b) + JOIN
        System.out.println("--- Scenario 1: type=batch, items=[x, y, z] ---");
        String wfId1 = client.startWorkflow("switch_fork_demo", 1,
                Map.of("type", "batch", "items", List.of("x", "y", "z")));
        System.out.println("  Workflow ID: " + wfId1);
        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Output: " + wf1.getOutput());
        if (!"COMPLETED".equals(wf1.getStatus().name())) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        // Scenario 2: Non-batch type -> single_process
        System.out.println("--- Scenario 2: type=single, items=[a] ---");
        String wfId2 = client.startWorkflow("switch_fork_demo", 1,
                Map.of("type", "single", "items", List.of("a")));
        System.out.println("  Workflow ID: " + wfId2);
        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        System.out.println("  Status: " + wf2.getStatus().name());
        System.out.println("  Output: " + wf2.getOutput());
        if (!"COMPLETED".equals(wf2.getStatus().name())) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        client.stopWorkers();

        if (allPassed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
