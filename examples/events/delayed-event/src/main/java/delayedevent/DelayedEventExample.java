package delayedevent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import delayedevent.workers.ReceiveEventWorker;
import delayedevent.workers.ComputeDelayWorker;
import delayedevent.workers.ApplyDelayWorker;
import delayedevent.workers.ProcessEventWorker;
import delayedevent.workers.LogCompletionWorker;

import java.util.List;
import java.util.Map;

/**
 * Delayed Event Processing Demo
 *
 * Demonstrates a sequential delayed event processing workflow:
 *   de_receive_event -> de_compute_delay -> de_apply_delay -> de_process_event -> de_log_completion
 *
 * Run:
 *   java -jar target/delayed-event-1.0.0.jar
 */
public class DelayedEventExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Delayed Event Processing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "de_receive_event", "de_compute_delay",
                "de_apply_delay", "de_process_event", "de_log_completion"));
        System.out.println("  Registered: de_receive_event, de_compute_delay, de_apply_delay, de_process_event, de_log_completion\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'delayed_event'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveEventWorker(),
                new ComputeDelayWorker(),
                new ApplyDelayWorker(),
                new ProcessEventWorker(),
                new LogCompletionWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("delayed_event", 1,
                Map.of("eventId", "delay-evt-300",
                        "payload", Map.of(
                                "type", "reminder",
                                "message", "Follow up with customer"),
                        "delaySeconds", 30));
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
