package competitiveagents;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import competitiveagents.workers.Solver1Worker;
import competitiveagents.workers.Solver2Worker;
import competitiveagents.workers.Solver3Worker;
import competitiveagents.workers.JudgeAgentWorker;
import competitiveagents.workers.SelectWinnerWorker;

import java.util.List;
import java.util.Map;

/**
 * Competitive Agents Demo — Three Solvers Compete
 *
 * Demonstrates a pattern where three solver agents propose competing solutions
 * in parallel (via FORK/JOIN), a judge agent scores and ranks them, and a
 * select-winner agent produces the final result.
 *
 * Run:
 *   java -jar target/competitive-agents-1.0.0.jar
 */
public class CompetitiveAgentsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Competitive Agents Demo: Three Solvers Compete ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "comp_solver_1", "comp_solver_2", "comp_solver_3",
                "comp_judge_agent", "comp_select_winner"));
        System.out.println("  Registered: comp_solver_1, comp_solver_2, comp_solver_3, comp_judge_agent, comp_select_winner\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'competitive_agents_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new Solver1Worker(),
                new Solver2Worker(),
                new Solver3Worker(),
                new JudgeAgentWorker(),
                new SelectWinnerWorker()
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
        String workflowId = client.startWorkflow("competitive_agents_demo", 1,
                Map.of("problem", "Reduce customer onboarding time by 50%",
                        "criteria", "cost,innovation,risk"));
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
