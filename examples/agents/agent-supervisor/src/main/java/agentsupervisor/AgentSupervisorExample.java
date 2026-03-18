package agentsupervisor;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agentsupervisor.workers.PlanWorker;
import agentsupervisor.workers.CoderAgentWorker;
import agentsupervisor.workers.TesterAgentWorker;
import agentsupervisor.workers.DocumenterAgentWorker;
import agentsupervisor.workers.ReviewWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 168: Agent Supervisor
 *
 * Orchestrates multiple specialized agents (coder, tester, documenter) under
 * a supervisor pattern. The plan worker creates a development plan, three
 * agents execute in parallel via FORK/JOIN, and a review worker produces
 * a final report.
 *
 * Run:
 *   java -jar target/agent-supervisor-1.0.0.jar
 *   java -jar target/agent-supervisor-1.0.0.jar --workers
 */
public class AgentSupervisorExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 168: Agent Supervisor ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sup_plan", "sup_coder_agent", "sup_tester_agent",
                "sup_documenter_agent", "sup_review"));
        System.out.println("  Registered: sup_plan, sup_coder_agent, sup_tester_agent, sup_documenter_agent, sup_review\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'agent_supervisor'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PlanWorker(),
                new CoderAgentWorker(),
                new TesterAgentWorker(),
                new DocumenterAgentWorker(),
                new ReviewWorker()
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
        String workflowId = client.startWorkflow("agent_supervisor", 1, Map.of(
                "feature", "user-authentication",
                "priority", "high",
                "systemPrompt", "You are a senior engineering supervisor coordinating agent work."
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Feature: " + workflow.getOutput().get("feature"));
        System.out.println("  Overall status: " + workflow.getOutput().get("overallStatus"));
        System.out.println("  Action items: " + workflow.getOutput().get("actionItems"));
        System.out.println("  Agents used: " + workflow.getOutput().get("agentsUsed"));

        System.out.println("\n--- Agent Supervisor Pattern ---");
        System.out.println("  - Plan: Create development plan with task assignments");
        System.out.println("  - FORK: Coder, tester, and documenter agents work in parallel");
        System.out.println("  - JOIN: Collect all agent results");
        System.out.println("  - Review: Supervisor reviews and produces final report");

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
