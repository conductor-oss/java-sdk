package scheduledevent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import scheduledevent.workers.QueueEventWorker;
import scheduledevent.workers.CheckScheduleWorker;
import scheduledevent.workers.WaitUntilReadyWorker;
import scheduledevent.workers.ExecuteEventWorker;
import scheduledevent.workers.ConfirmWorker;

import java.util.List;
import java.util.Map;

/**
 * Scheduled Event Demo
 *
 * Demonstrates a sequential scheduled-event workflow:
 *   se_queue_event -> se_check_schedule -> se_wait_until_ready -> se_execute_event -> se_confirm
 *
 * Run:
 *   java -jar target/scheduled-event-1.0.0.jar
 */
public class ScheduledEventExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Scheduled Event Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "se_queue_event", "se_check_schedule",
                "se_wait_until_ready", "se_execute_event", "se_confirm"));
        System.out.println("  Registered: se_queue_event, se_check_schedule, se_wait_until_ready, se_execute_event, se_confirm\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'scheduled_event_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new QueueEventWorker(),
                new CheckScheduleWorker(),
                new WaitUntilReadyWorker(),
                new ExecuteEventWorker(),
                new ConfirmWorker()
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
        String workflowId = client.startWorkflow("scheduled_event_wf", 1,
                Map.of("eventId", "evt-sched-001",
                        "payload", Map.of(
                                "action", "send-report",
                                "recipient", "admin@example.com"),
                        "scheduledTime", "2026-01-15T10:00:00Z"));
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
