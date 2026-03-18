package agentcollaboration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agentcollaboration.workers.AnalystWorker;
import agentcollaboration.workers.StrategistWorker;
import agentcollaboration.workers.ExecutorWorker;
import agentcollaboration.workers.CompilePlanWorker;

import java.util.List;
import java.util.Map;

/**
 * Agent Collaboration Demo — Sequential Pipeline
 *
 * Demonstrates a pattern where four specialist agents collaborate sequentially:
 * analyst produces insights, strategist builds a strategy, executor creates
 * action items, and a compiler assembles the final plan.
 *
 * Run:
 *   java -jar target/agent-collaboration-1.0.0.jar
 */
public class AgentCollaborationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Agent Collaboration Demo: Sequential Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ac_analyst", "ac_strategist", "ac_executor", "ac_compile_plan"));
        System.out.println("  Registered: ac_analyst, ac_strategist, ac_executor, ac_compile_plan\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'agent_collaboration_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalystWorker(),
                new StrategistWorker(),
                new ExecutorWorker(),
                new CompilePlanWorker()
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
        String workflowId = client.startWorkflow("agent_collaboration_demo", 1,
                Map.of("businessContext", "E-commerce platform experiencing 15% customer churn rate with declining repeat purchases"));
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
