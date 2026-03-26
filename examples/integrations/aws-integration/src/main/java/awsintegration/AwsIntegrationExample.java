package awsintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import awsintegration.workers.AwsS3UploadWorker;
import awsintegration.workers.AwsDynamoDbWriteWorker;
import awsintegration.workers.AwsSnsNotifyWorker;
import awsintegration.workers.AwsVerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * AWS Integration Demo
 *
 * Runs an AWS multi-service integration workflow using FORK_JOIN:
 *   FORK(S3 upload, DynamoDB write, SNS notify) -> JOIN -> verify.
 *
 * Run:
 *   java -jar target/aws-integration-1.0.0.jar
 */
public class AwsIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 441: AWS Integration ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "aws_s3_upload", "aws_dynamodb_write", "aws_sns_notify", "aws_verify"));
        System.out.println("  Registered: aws_s3_upload, aws_dynamodb_write, aws_sns_notify, aws_verify\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'aws_integration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AwsS3UploadWorker(),
                new AwsDynamoDbWriteWorker(),
                new AwsSnsNotifyWorker(),
                new AwsVerifyWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("aws_integration", 1,
                Map.of("bucket", "my-data-bucket",
                        "tableName", "events-table",
                        "topicArn", "arn:aws:sns:us-east-1:123456789:data-events",
                        "payload", Map.of(
                                "id", "evt-5001",
                                "type", "order.created",
                                "timestamp", "2026-01-15T10:00:00Z",
                                "data", Map.of("orderId", "ORD-789", "amount", 149.99))));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
