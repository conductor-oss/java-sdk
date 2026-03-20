package amazonbedrock;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import amazonbedrock.workers.BedrockBuildPayloadWorker;
import amazonbedrock.workers.BedrockInvokeModelWorker;
import amazonbedrock.workers.BedrockParseOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 115: Orchestrating Amazon Bedrock Calls
 *
 * Demonstrates how to orchestrate Amazon Bedrock InvokeModel calls through
 * Conductor. The worker calls Bedrock's invoke API for Claude on Bedrock,
 * showing the AWS-specific request format with modelId and accept/contentType.
 *
 * Run:
 *   java -jar target/amazon-bedrock-1.0.0.jar
 */
public class AmazonBedrockExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 115: Orchestrating Amazon Bedrock ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "bedrock_build_payload", "bedrock_invoke_model", "bedrock_parse_output"));
        System.out.println("  Registered: bedrock_build_payload, bedrock_invoke_model, bedrock_parse_output\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'amazon_bedrock_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new BedrockBuildPayloadWorker(),
                new BedrockInvokeModelWorker(),
                new BedrockParseOutputWorker());
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("amazon_bedrock_workflow", 1,
                Map.of(
                        "prompt", "Classify this support ticket and determine required actions: "
                                + "'Customer reports unauthorized access to their account with personal "
                                + "data viewed by unknown party.'",
                        "useCase", "Support ticket triage and compliance classification"
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Model: " + workflow.getOutput().get("modelId"));
        System.out.println("  Latency: " + workflow.getOutput().get("latency") + "ms");
        String classification = (String) workflow.getOutput().get("classification");
        if (classification != null) {
            System.out.println("  Classification: " + classification.substring(0, Math.min(60, classification.length())) + "...");
        }
        System.out.println("  Tokens: " + workflow.getOutput().get("tokens"));

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
