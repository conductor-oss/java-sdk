package planexecuteagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import planexecuteagent.workers.CreatePlanWorker;
import planexecuteagent.workers.ExecuteStep1Worker;
import planexecuteagent.workers.ExecuteStep2Worker;
import planexecuteagent.workers.ExecuteStep3Worker;
import planexecuteagent.workers.CompileResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * Plan-Execute Agent Demo
 *
 * Demonstrates a sequential pipeline of five workers that implement the
 * plan-and-execute pattern: create a plan from an objective, execute each
 * step sequentially, and compile the results into a final report.
 *   pe_create_plan -> pe_execute_step_1 -> pe_execute_step_2 -> pe_execute_step_3 -> pe_compile_results
 *
 * Run:
 *   java -jar target/plan-execute-agent-1.0.0.jar
 */
public class PlanExecuteAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Plan-Execute Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pe_create_plan", "pe_execute_step_1",
                "pe_execute_step_2", "pe_execute_step_3",
                "pe_compile_results"));
        System.out.println("  Registered: pe_create_plan, pe_execute_step_1, pe_execute_step_2, pe_execute_step_3, pe_compile_results\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'plan_execute_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreatePlanWorker(),
                new ExecuteStep1Worker(),
                new ExecuteStep2Worker(),
                new ExecuteStep3Worker(),
                new CompileResultsWorker()
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
        String workflowId = client.startWorkflow("plan_execute_agent", 1,
                Map.of("objective", "Develop go-to-market strategy for new SaaS product"));
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
