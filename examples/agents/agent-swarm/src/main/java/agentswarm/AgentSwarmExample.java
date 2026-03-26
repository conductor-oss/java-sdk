package agentswarm;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agentswarm.workers.DecomposeWorker;
import agentswarm.workers.Swarm1Worker;
import agentswarm.workers.Swarm2Worker;
import agentswarm.workers.Swarm3Worker;
import agentswarm.workers.Swarm4Worker;
import agentswarm.workers.MergeWorker;

import java.util.List;
import java.util.Map;

/**
 * Agent Swarm Demo — Decompose, Parallel Swarm, Merge
 *
 * Demonstrates a pattern where a decompose agent breaks a research topic into
 * subtasks, four swarm agents work on them in parallel (via FORK/JOIN), and a
 * merge agent combines the findings into a unified report.
 *
 * Run:
 *   java -jar target/agent-swarm-1.0.0.jar
 */
public class AgentSwarmExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Agent Swarm Demo: Decompose, Parallel Swarm, Merge ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "as_decompose", "as_swarm_1", "as_swarm_2",
                "as_swarm_3", "as_swarm_4", "as_merge"));
        System.out.println("  Registered: as_decompose, as_swarm_1, as_swarm_2, as_swarm_3, as_swarm_4, as_merge\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'agent_swarm_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DecomposeWorker(),
                new Swarm1Worker(),
                new Swarm2Worker(),
                new Swarm3Worker(),
                new Swarm4Worker(),
                new MergeWorker()
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
        String workflowId = client.startWorkflow("agent_swarm_demo", 1,
                Map.of("researchTopic", "The impact of large language models on software engineering"));
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
