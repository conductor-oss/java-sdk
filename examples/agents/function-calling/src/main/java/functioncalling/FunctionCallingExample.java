package functioncalling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import functioncalling.workers.LlmPlanWorker;
import functioncalling.workers.ExtractFunctionCallWorker;
import functioncalling.workers.ExecuteFunctionWorker;
import functioncalling.workers.LlmSynthesizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Function Calling Demo
 *
 * Demonstrates a sequential LLM function-calling pipeline:
 *   llm_plan -> extract_function_call -> execute_function -> llm_synthesize
 *
 * Run:
 *   java -jar target/function-calling-1.0.0.jar
 */
public class FunctionCallingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Function Calling Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "fc_llm_plan", "fc_extract_function_call",
                "fc_execute_function", "fc_llm_synthesize"));
        System.out.println("  Registered: fc_llm_plan, fc_extract_function_call, fc_execute_function, fc_llm_synthesize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'function_calling'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LlmPlanWorker(),
                new ExtractFunctionCallWorker(),
                new ExecuteFunctionWorker(),
                new LlmSynthesizeWorker()
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

        List<Map<String, Object>> functionDefinitions = List.of(
                Map.of("name", "get_stock_price",
                        "description", "Get the current stock price for a given ticker symbol",
                        "parameters", Map.of(
                                "ticker", Map.of("type", "string", "description", "Stock ticker symbol"),
                                "includeChange", Map.of("type", "boolean", "description", "Include price change data"))),
                Map.of("name", "get_weather",
                        "description", "Get the current weather for a location",
                        "parameters", Map.of(
                                "location", Map.of("type", "string", "description", "City name or coordinates"),
                                "units", Map.of("type", "string", "description", "Temperature units: celsius or fahrenheit"))),
                Map.of("name", "search_web",
                        "description", "Search the web for information",
                        "parameters", Map.of(
                                "query", Map.of("type", "string", "description", "Search query"),
                                "maxResults", Map.of("type", "integer", "description", "Maximum number of results")))
        );

        String workflowId = client.startWorkflow("function_calling", 1,
                Map.of("userQuery", "What's the current price of Apple stock?",
                        "functionDefinitions", functionDefinitions));
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
