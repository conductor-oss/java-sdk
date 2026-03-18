package tooluse;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tooluse.workers.AnalyzeRequestWorker;
import tooluse.workers.SelectToolWorker;
import tooluse.workers.ExecuteToolWorker;
import tooluse.workers.FormatResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Basics Demo
 *
 * Demonstrates a sequential pipeline of four workers that demonstrate tool use:
 *   analyze_request -> select_tool -> execute_tool -> format_result
 *
 * Run:
 *   java -jar target/tool-use-basics-1.0.0.jar
 */
public class ToolUseBasicsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Basics Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tu_analyze_request", "tu_select_tool", "tu_execute_tool", "tu_format_result"));
        System.out.println("  Registered: tu_analyze_request, tu_select_tool, tu_execute_tool, tu_format_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_basics'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalyzeRequestWorker(),
                new SelectToolWorker(),
                new ExecuteToolWorker(),
                new FormatResultWorker()
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
        String workflowId = client.startWorkflow("tool_use_basics", 1,
                Map.of("userRequest", "What's the weather like in San Francisco?",
                        "availableTools", List.of(
                                Map.of("name", "weather_api",
                                        "description", "Fetches current weather data",
                                        "parameters", List.of("location", "units")),
                                Map.of("name", "calculator",
                                        "description", "Performs mathematical calculations",
                                        "parameters", List.of("expression", "precision")),
                                Map.of("name", "web_search",
                                        "description", "Searches the web for information",
                                        "parameters", List.of("query", "maxResults"))
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
