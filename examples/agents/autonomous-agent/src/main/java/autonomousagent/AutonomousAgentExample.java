package autonomousagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import autonomousagent.workers.SetGoalWorker;
import autonomousagent.workers.CreatePlanWorker;
import autonomousagent.workers.ExecuteStepWorker;
import autonomousagent.workers.EvaluateProgressWorker;
import autonomousagent.workers.FinalReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Autonomous Agent Demo
 *
 * Demonstrates an autonomous agent that sets a goal, creates a plan,
 * executes steps in a loop with progress evaluation, then produces a final report:
 *   set_goal -> create_plan -> DO_WHILE(execute_step -> evaluate_progress, 3 iters) -> final_report
 *
 * Run:
 *   java -jar target/autonomous-agent-1.0.0.jar
 */
public class AutonomousAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Autonomous Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "aa_set_goal", "aa_create_plan", "aa_execute_step",
                "aa_evaluate_progress", "aa_final_report"));
        System.out.println("  Registered: aa_set_goal, aa_create_plan, aa_execute_step, aa_evaluate_progress, aa_final_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'autonomous_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SetGoalWorker(),
                new CreatePlanWorker(),
                new ExecuteStepWorker(),
                new EvaluateProgressWorker(),
                new FinalReportWorker()
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
        String workflowId = client.startWorkflow("autonomous_agent", 1,
                Map.of("mission", "Set up production monitoring for the platform"));
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
