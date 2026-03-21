package apigateway;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apigateway.workers.AgAuthenticateWorker;
import apigateway.workers.RouteRequestWorker;
import apigateway.workers.TransformResponseWorker;
import apigateway.workers.SendResponseWorker;

import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * API Gateway Pattern Demo
 *
 * Demonstrates an API gateway workflow:
 *   ag_authenticate -> ag_route_request -> ag_transform_response -> ag_send_response
 *
 * Run:
 *   java -jar target/api-gateway-1.0.0.jar
 */
public class ApiGatewayExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== API Gateway Pattern Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ag_authenticate", "ag_route_request", "ag_transform_response", "ag_send_response"));

        System.out.println("Step 2: Registering workflow 'api_gateway_292'...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AgAuthenticateWorker(), new RouteRequestWorker(),
                new TransformResponseWorker(), new SendResponseWorker());
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...");
        
        // Store secret via Conductor Secrets API
        String conductorUrl = System.getenv().getOrDefault("CONDUCTOR_BASE_URL", "http://localhost:8080/api");
        HttpClient http = HttpClient.newHttpClient();
        http.send(HttpRequest.newBuilder()
                .uri(URI.create(conductorUrl + "/secrets/gateway_api_key"))
                .PUT(HttpRequest.BodyPublishers.ofString("\"sk-test-gateway-key\""))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("  Secret \'gateway_api_key\' stored via Conductor Secrets API");

        String workflowId = client.startWorkflow("api_gateway_292", 1,
                Map.of("endpoint", "/api/v1/users", "method", "GET", "payload", Map.of()));

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
