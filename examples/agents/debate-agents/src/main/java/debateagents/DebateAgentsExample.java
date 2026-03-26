package debateagents;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import debateagents.workers.SetTopicWorker;
import debateagents.workers.AgentProWorker;
import debateagents.workers.AgentConWorker;
import debateagents.workers.ModeratorSummarizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Debate Agents Demo — PRO vs CON over Multiple Rounds
 *
 * Demonstrates a pattern where a topic is set, then PRO and CON agents
 * argue over it in a DO_WHILE loop for 3 rounds, followed by a moderator
 * that summarizes the debate and delivers a verdict.
 *
 * Run:
 *   java -jar target/debate-agents-1.0.0.jar
 */
public class DebateAgentsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Debate Agents Demo: PRO vs CON ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "da_set_topic", "da_agent_pro", "da_agent_con", "da_moderator_summarize"));
        System.out.println("  Registered: da_set_topic, da_agent_pro, da_agent_con, da_moderator_summarize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'debate_agents_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SetTopicWorker(),
                new AgentProWorker(),
                new AgentConWorker(),
                new ModeratorSummarizeWorker()
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
        String workflowId = client.startWorkflow("debate_agents_demo", 1,
                Map.of("topic", "Microservices vs Monolithic Architecture"));
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
