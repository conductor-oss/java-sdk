package apicalling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apicalling.workers.PlanApiCallWorker;
import apicalling.workers.AuthenticateWorker;
import apicalling.workers.CallApiWorker;
import apicalling.workers.ParseResponseWorker;
import apicalling.workers.FormatOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * API Calling Agent Demo
 *
 * Demonstrates a sequential pipeline of five workers that collaborate
 * to plan, authenticate, call, parse, and format API interactions:
 *   plan_api_call -> authenticate -> call_api -> parse_response -> format_output
 *
 * Run:
 *   java -jar target/api-calling-agent-1.0.0.jar
 */
public class ApiCallingAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== API Calling Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ap_plan_api_call", "ap_authenticate", "ap_call_api",
                "ap_parse_response", "ap_format_output"));
        System.out.println("  Registered: ap_plan_api_call, ap_authenticate, ap_call_api, ap_parse_response, ap_format_output\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'api_calling_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PlanApiCallWorker(),
                new AuthenticateWorker(),
                new CallApiWorker(),
                new ParseResponseWorker(),
                new FormatOutputWorker()
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

        List<Map<String, Object>> apiCatalog = List.of(
                Map.of("name", "github", "baseUrl", "https://api.github.com",
                        "description", "GitHub REST API for repository and user data"),
                Map.of("name", "weather", "baseUrl", "https://api.openweathermap.org",
                        "description", "OpenWeatherMap API for weather forecasts"),
                Map.of("name", "news", "baseUrl", "https://newsapi.org",
                        "description", "News API for top headlines and article search")
        );

        String workflowId = client.startWorkflow("api_calling_agent", 1,
                Map.of("userRequest", "Tell me about the Conductor open-source repository on GitHub",
                        "apiCatalog", apiCatalog));
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
