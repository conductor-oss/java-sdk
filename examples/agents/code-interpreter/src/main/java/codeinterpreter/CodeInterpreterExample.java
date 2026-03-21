package codeinterpreter;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import codeinterpreter.workers.AnalyzeQuestionWorker;
import codeinterpreter.workers.GenerateCodeWorker;
import codeinterpreter.workers.ExecuteSandboxWorker;
import codeinterpreter.workers.InterpretResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Code Interpreter Agent Demo
 *
 * Demonstrates a sequential pipeline of four workers that collaborate
 * to analyze a data question, generate Python code, execute it in a sandbox,
 * and interpret the results:
 *   analyze_question -> generate_code -> execute_sandbox -> interpret_result
 *
 * Run:
 *   java -jar target/code-interpreter-1.0.0.jar
 */
public class CodeInterpreterExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Code Interpreter Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ci_analyze_question", "ci_generate_code",
                "ci_execute_sandbox", "ci_interpret_result"));
        System.out.println("  Registered: ci_analyze_question, ci_generate_code, ci_execute_sandbox, ci_interpret_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'code_interpreter_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalyzeQuestionWorker(),
                new GenerateCodeWorker(),
                new ExecuteSandboxWorker(),
                new InterpretResultWorker()
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
        String workflowId = client.startWorkflow("code_interpreter_agent", 1,
                Map.of("question", "What is the average sales by region? Which region performs best?",
                        "dataset", Map.of(
                                "name", "quarterly_sales",
                                "rows", 100,
                                "columns", List.of("region", "product", "sales", "quarter", "units"),
                                "sampleRow", Map.of(
                                        "region", "West",
                                        "product", "Widget A",
                                        "sales", 42500,
                                        "quarter", "Q1",
                                        "units", 150
                                )
                        )));
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
