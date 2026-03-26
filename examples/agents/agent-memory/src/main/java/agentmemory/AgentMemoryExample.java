package agentmemory;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agentmemory.workers.LoadMemoryWorker;
import agentmemory.workers.AgentThinkWorker;
import agentmemory.workers.UpdateMemoryWorker;
import agentmemory.workers.AgentRespondWorker;

import java.util.List;
import java.util.Map;

/**
 * Agent with Memory Demo
 *
 * Demonstrates a sequential agent pattern that loads conversation history,
 * thinks with context, updates its memory store, and produces a response.
 *
 * Run:
 *   java -jar target/agent-memory-1.0.0.jar
 */
public class AgentMemoryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Agent with Memory Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "am_load_memory", "am_agent_think",
                "am_update_memory", "am_agent_respond"));
        System.out.println("  Registered: am_load_memory, am_agent_think, am_update_memory, am_agent_respond\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'agent_memory_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadMemoryWorker(),
                new AgentThinkWorker(),
                new UpdateMemoryWorker(),
                new AgentRespondWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("agent_memory_demo", 1,
                Map.of("userId", "user-42",
                        "userMessage", "Can you explain how transformers work in deep learning?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
