package agenticloop;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agenticloop.workers.SetGoalWorker;
import agenticloop.workers.ThinkWorker;
import agenticloop.workers.ActWorker;
import agenticloop.workers.ObserveWorker;
import agenticloop.workers.SummarizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Agentic Loop Demo
 *
 * Demonstrates a think-act-observe loop pattern:
 * set_goal -> DO_WHILE(think -> act -> observe, 3 iterations) -> summarize
 *
 * Run:
 *   java -jar target/agentic-loop-1.0.0.jar
 */
public class AgenticLoopExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Agentic Loop Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "al_set_goal", "al_think", "al_act", "al_observe", "al_summarize"));
        System.out.println("  Registered: al_set_goal, al_think, al_act, al_observe, al_summarize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'agentic_loop'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SetGoalWorker(),
                new ThinkWorker(),
                new ActWorker(),
                new ObserveWorker(),
                new SummarizeWorker()
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
        String workflowId = client.startWorkflow("agentic_loop", 1,
                Map.of("goal", "Research best practices for distributed systems"));
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
