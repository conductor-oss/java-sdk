package tooluseparallel;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tooluseparallel.workers.PlanToolsWorker;
import tooluseparallel.workers.CallWeatherWorker;
import tooluseparallel.workers.CallNewsWorker;
import tooluseparallel.workers.CallStocksWorker;
import tooluseparallel.workers.CombineResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Parallel Demo
 *
 * Demonstrates parallel tool calls using FORK_JOIN: an LLM-style plan step
 * determines which tools to call, then weather/news/stocks APIs are called
 * in parallel, and finally the results are combined into a morning briefing.
 *   plan_tools -> FORK(call_weather, call_news, call_stocks) -> JOIN -> combine_results
 *
 * Run:
 *   java -jar target/tool-use-parallel-1.0.0.jar
 */
public class ToolUseParallelExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Parallel Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tp_plan_tools", "tp_call_weather", "tp_call_news",
                "tp_call_stocks", "tp_combine_results"));
        System.out.println("  Registered: tp_plan_tools, tp_call_weather, tp_call_news, tp_call_stocks, tp_combine_results\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_parallel'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PlanToolsWorker(),
                new CallWeatherWorker(),
                new CallNewsWorker(),
                new CallStocksWorker(),
                new CombineResultsWorker()
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
        String workflowId = client.startWorkflow("tool_use_parallel", 1,
                Map.of("userRequest", "Give me my morning briefing",
                        "location", "San Francisco, CA"));
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
