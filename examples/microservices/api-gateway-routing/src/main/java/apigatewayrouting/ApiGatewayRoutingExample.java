package apigatewayrouting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apigatewayrouting.workers.AuthenticateWorker;
import apigatewayrouting.workers.RateCheckWorker;
import apigatewayrouting.workers.RouteRequestWorker;
import apigatewayrouting.workers.TransformResponseWorker;

import java.util.List;
import java.util.Map;

/**
 * API Gateway Routing Demo
 *
 * Routes API requests through authentication, rate limiting, service selection, and response transformation.
 */
public class ApiGatewayRoutingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== API Gateway Routing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("gw_authenticate", "gw_rate_check", "gw_route_request", "gw_transform_response"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AuthenticateWorker(), new RateCheckWorker(),
                new RouteRequestWorker(), new TransformResponseWorker());
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...");
        String workflowId = client.startWorkflow("api_gateway_routing", 1,
                Map.of("path", "/api/orders/123", "method", "GET",
                        "headers", Map.of("authorization", "Bearer token123"),
                        "body", Map.of()));

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
