package eventttl;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventttl.workers.CheckExpiryWorker;
import eventttl.workers.ProcessEventWorker;
import eventttl.workers.AcknowledgeWorker;
import eventttl.workers.LogExpiredWorker;

import java.util.List;
import java.util.Map;

/**
 * Event TTL (Time-to-Live) Demo
 *
 * Demonstrates a SWITCH-based event TTL workflow:
 *   xl_check_expiry -> SWITCH(status:
 *       valid   -> xl_process_event -> xl_acknowledge,
 *       expired -> xl_log_expired)
 *
 * Run:
 *   java -jar target/event-ttl-1.0.0.jar
 */
public class EventTtlExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event TTL Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "xl_check_expiry", "xl_process_event",
                "xl_acknowledge", "xl_log_expired"));
        System.out.println("  Registered: xl_check_expiry, xl_process_event, xl_acknowledge, xl_log_expired\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_ttl'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckExpiryWorker(),
                new ProcessEventWorker(),
                new AcknowledgeWorker(),
                new LogExpiredWorker()
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
        String workflowId = client.startWorkflow("event_ttl", 1,
                Map.of("eventId", "ttl-evt-400",
                        "payload", Map.of("action", "sync_data"),
                        "ttlSeconds", 600,
                        "createdAt", "2026-01-15T09:58:00Z"));
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
