package deadletterevents;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import deadletterevents.workers.DlReceiveEventWorker;
import deadletterevents.workers.DlAttemptProcessWorker;
import deadletterevents.workers.DlFinalizeSuccessWorker;
import deadletterevents.workers.DlRouteToDlqWorker;
import deadletterevents.workers.DlSendAlertWorker;

import java.util.List;
import java.util.Map;

/**
 * Dead Letter Events Demo
 *
 * Demonstrates a dead letter queue pattern: receive an event, attempt processing,
 * and on failure route to DLQ with alerting via a SWITCH task.
 *   dl_receive_event -> dl_attempt_process -> SWITCH(processingResult)
 *     success -> dl_finalize_success
 *     default -> dl_route_to_dlq -> dl_send_alert
 *
 * Run:
 *   java -jar target/dead-letter-events-1.0.0.jar
 */
public class DeadLetterEventsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Dead Letter Events Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dl_receive_event", "dl_attempt_process",
                "dl_finalize_success", "dl_route_to_dlq", "dl_send_alert"));
        System.out.println("  Registered: dl_receive_event, dl_attempt_process, dl_finalize_success, dl_route_to_dlq, dl_send_alert\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'dead_letter_events_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DlReceiveEventWorker(),
                new DlAttemptProcessWorker(),
                new DlFinalizeSuccessWorker(),
                new DlRouteToDlqWorker(),
                new DlSendAlertWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("dead_letter_events_wf", 1,
                Map.of("eventId", "evt-fixed-001",
                        "eventType", "payment.charge",
                        "payload", Map.of("amount", 99.99, "currency", "USD"),
                        "retryCount", 3));
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
