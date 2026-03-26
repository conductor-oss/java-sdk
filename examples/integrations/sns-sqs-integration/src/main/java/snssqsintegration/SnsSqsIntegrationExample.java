package snssqsintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import snssqsintegration.workers.SnsPublishWorker;
import snssqsintegration.workers.SubscribeQueueWorker;
import snssqsintegration.workers.ReceiveMessageWorker;
import snssqsintegration.workers.ProcessMessageWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 448: SNS + SQS Integration
 *
 * Runs an SNS/SQS messaging integration workflow:
 * publish to SNS -> subscribe queue -> receive message -> process.
 *
 * Run:
 *   java -jar target/sns-sqs-integration-1.0.0.jar
 */
public class SnsSqsIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 448: SNS + SQS Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sns_publish", "sns_subscribe_queue", "sns_receive_message", "sns_process_message"));
        System.out.println("  Registered: sns_publish, sns_subscribe_queue, sns_receive_message, sns_process_message\n");

        System.out.println("Step 2: Registering workflow \'sns_sqs_integration_448\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SnsPublishWorker(),
                new SubscribeQueueWorker(),
                new ReceiveMessageWorker(),
                new ProcessMessageWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("sns_sqs_integration_448", 1,
                Map.of("topicArn", "arn:aws:sns:us-east-1:123456789:order-events",
                        "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/order-processing",
                        "messageBody", "order-shipped-event"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  SNS Message ID: " + workflow.getOutput().get("snsMessageId"));
        System.out.println("  Subscription ARN: " + workflow.getOutput().get("subscriptionArn"));
        System.out.println("  Processed: " + workflow.getOutput().get("processed"));

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
