package tooluseconditional;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tooluseconditional.workers.ClassifyQueryWorker;
import tooluseconditional.workers.CalculatorWorker;
import tooluseconditional.workers.InterpreterWorker;
import tooluseconditional.workers.WebSearchWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use — Conditional Tool Selection Demo
 *
 * Demonstrates classifying a user query and conditionally routing to the
 * appropriate tool via a SWITCH task:
 *   classify_query -> SWITCH(math->calculator, code->interpreter, search->web_search)
 *   defaultCase -> web_search
 *
 * Run:
 *   java -jar target/tool-use-conditional-1.0.0.jar
 */
public class ToolUseConditionalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use — Conditional Tool Selection Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tc_classify_query", "tc_calculator", "tc_interpreter", "tc_web_search"));
        System.out.println("  Registered: tc_classify_query, tc_calculator, tc_interpreter, tc_web_search\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_conditional'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyQueryWorker(),
                new CalculatorWorker(),
                new InterpreterWorker(),
                new WebSearchWorker()
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
        String workflowId = client.startWorkflow("tool_use_conditional", 1,
                Map.of("userQuery", "Calculate the square root of 144"));
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
