package reactagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import reactagent.workers.InitTaskWorker;
import reactagent.workers.ReasonWorker;
import reactagent.workers.ActWorker;
import reactagent.workers.ObserveWorker;
import reactagent.workers.FinalAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * ReAct Agent Demo
 *
 * Demonstrates the Reason-Act-Observe pattern using a DO_WHILE loop:
 *   rx_init_task -> DO_WHILE(rx_reason -> rx_act -> rx_observe) -> rx_final_answer
 *
 * The agent iteratively reasons about a question, takes actions (search/synthesize),
 * observes results, and produces a final answer after 3 iterations.
 *
 * Run:
 *   java -jar target/react-agent-1.0.0.jar
 */
public class ReactAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== ReAct Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rx_init_task", "rx_reason", "rx_act",
                "rx_observe", "rx_final_answer"));
        System.out.println("  Registered: rx_init_task, rx_reason, rx_act, rx_observe, rx_final_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'react_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new InitTaskWorker(),
                new ReasonWorker(),
                new ActWorker(),
                new ObserveWorker(),
                new FinalAnswerWorker()
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
        String workflowId = client.startWorkflow("react_agent", 1,
                Map.of("question", "What is the current world population?"));
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
