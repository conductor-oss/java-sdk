package pubsubconsumer;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import pubsubconsumer.workers.PsReceiveMessageWorker;
import pubsubconsumer.workers.PsDecodePayloadWorker;
import pubsubconsumer.workers.PsProcessDataWorker;
import pubsubconsumer.workers.PsAckMessageWorker;

import java.util.List;
import java.util.Map;

/**
 * Pub/Sub Consumer Demo
 *
 * Demonstrates a sequential pipeline of four workers that process a Pub/Sub message:
 * receive message, decode payload, process sensor data, and acknowledge.
 *   ps_receive_message -> ps_decode_payload -> ps_process_data -> ps_ack_message
 *
 * Run:
 *   java -jar target/pubsub-consumer-1.0.0.jar
 */
public class PubsubConsumerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Pub/Sub Consumer Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ps_receive_message", "ps_decode_payload",
                "ps_process_data", "ps_ack_message"));
        System.out.println("  Registered: ps_receive_message, ps_decode_payload, ps_process_data, ps_ack_message\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'pubsub_consumer_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PsReceiveMessageWorker(),
                new PsDecodePayloadWorker(),
                new PsProcessDataWorker(),
                new PsAckMessageWorker()
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
        String workflowId = client.startWorkflow("pubsub_consumer_wf", 1,
                Map.of("subscription", "projects/my-project/subscriptions/sensor-readings-sub",
                        "messageId", "ps-fixed-001",
                        "publishTime", "2026-03-08T10:15:00Z",
                        "data", "eyJzZW5zb3JJZCI6...",
                        "attributes", Map.of(
                                "eventType", "iot.sensor.reading",
                                "sensorType", "environmental")));
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
