package sqsconsumer;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sqsconsumer.workers.ReceiveMessageWorker;
import sqsconsumer.workers.ValidateMessageWorker;
import sqsconsumer.workers.ProcessMessageWorker;
import sqsconsumer.workers.DeleteMessageWorker;

import java.util.List;
import java.util.Map;

/**
 * SQS Consumer Demo
 *
 * Demonstrates a sequential pipeline of four workers that consume an SQS message:
 * receive the message, validate it, process the event, and delete it from the queue.
 *   qs_receive_message -> qs_validate_message -> qs_process_message -> qs_delete_message
 *
 * Run:
 *   java -jar target/sqs-consumer-1.0.0.jar
 */
public class SqsConsumerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SQS Consumer Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "qs_receive_message", "qs_validate_message",
                "qs_process_message", "qs_delete_message"));
        System.out.println("  Registered: qs_receive_message, qs_validate_message, qs_process_message, qs_delete_message\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'sqs_consumer_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveMessageWorker(),
                new ValidateMessageWorker(),
                new ProcessMessageWorker(),
                new DeleteMessageWorker()
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
        String workflowId = client.startWorkflow("sqs_consumer_wf", 1,
                Map.of("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/invoices-queue",
                        "messageId", "msg-fixed-001",
                        "receiptHandle", "AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...",
                        "body", "{\"eventType\":\"invoice.generated\",\"invoiceId\":\"INV-2026-0831\",\"customerId\":\"C-2244\",\"amount\":4500.00,\"dueDate\":\"2026-04-07\"}"));
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
