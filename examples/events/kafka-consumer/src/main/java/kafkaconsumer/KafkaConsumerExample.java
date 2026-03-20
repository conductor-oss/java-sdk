package kafkaconsumer;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import kafkaconsumer.workers.*;

import java.util.*;

/**
 * Kafka Consumer Pipeline — Message Processing Workflow
 *
 * Performs a Kafka consumer that receives a message, deserializes it,
 * processes the payload, and commits the offset.
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/kafka-consumer-1.0.0.jar
 */
public class KafkaConsumerExample {

    private static final List<String> TASK_NAMES = List.of(
            "kc_receive_message",
            "kc_deserialize",
            "kc_process_payload",
            "kc_commit_offset"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new ReceiveMessage(),
                new Deserialize(),
                new ProcessPayload(),
                new CommitOffset()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Kafka Consumer Demo: Message Processing Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'kafka_consumer_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Use the Conductor CLI or UI to start workflows.\n");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Allow workers to start polling
        Thread.sleep(2000);

        // Step 4 — Run the consumer pipeline
        System.out.println("Step 4: Running Kafka consumer pipeline...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("topic", "user-events");
        input.put("partition", 3);
        input.put("offset", "14582");
        input.put("messageKey", "U-4421");
        input.put("messageValue", "{\"userId\":\"U-4421\",\"action\":\"profile_updated\"}");

        String workflowId = client.startWorkflow("kafka_consumer_wf", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Consumer Results ---");
            System.out.println("  Topic        : " + output.get("topic"));
            System.out.println("  Partition    : " + output.get("partition"));
            System.out.println("  Offset       : " + output.get("offset"));
            System.out.println("  Message Type : " + output.get("messageType"));
            System.out.println("  Processed    : " + output.get("processed"));
            System.out.println("  Committed    : " + output.get("committed"));
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nWorkflow did not complete (status: " + status + ")");
            workflow.getTasks().stream()
                    .filter(t -> t.getStatus().name().equals("FAILED"))
                    .forEach(t -> System.out.println("  Failed task: " + t.getReferenceTaskName()
                            + " — " + t.getReasonForIncompletion()));
            System.out.println("Result: WORKFLOW_ERROR");
            System.exit(1);
        }

        System.out.println("\nResult: SUCCESS — message consumed, processed, and offset committed");
        System.exit(0);
    }
}
