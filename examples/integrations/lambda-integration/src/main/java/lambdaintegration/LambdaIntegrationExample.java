package lambdaintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import lambdaintegration.workers.PreparePayloadWorker;
import lambdaintegration.workers.InvokeLambdaWorker;
import lambdaintegration.workers.ProcessResponseWorker;
import lambdaintegration.workers.LogResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 449: Lambda Integration
 *
 * Runs an AWS Lambda integration workflow:
 * prepare payload -> invoke function -> process response -> log result.
 *
 * Run:
 *   java -jar target/lambda-integration-1.0.0.jar
 */
public class LambdaIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 449: Lambda Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "lam_prepare_payload", "lam_invoke", "lam_process_response", "lam_log_result"));
        System.out.println("  Registered: lam_prepare_payload, lam_invoke, lam_process_response, lam_log_result\n");

        System.out.println("Step 2: Registering workflow \'lambda_integration_449\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PreparePayloadWorker(),
                new InvokeLambdaWorker(),
                new ProcessResponseWorker(),
                new LogResultWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("lambda_integration_449", 1,
                Map.of("functionName", "process-orders",
                        "qualifier", "$LATEST",
                        "inputData", Map.of("action", "processNewOrders", "region", "us-east-1", "batchSize", 50)));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Function: " + workflow.getOutput().get("functionName"));
        System.out.println("  Status Code: " + workflow.getOutput().get("statusCode"));
        System.out.println("  Result: " + workflow.getOutput().get("result"));
        System.out.println("  Logged: " + workflow.getOutput().get("logged"));

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
