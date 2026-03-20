package eventaudittrail;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventaudittrail.workers.LogReceivedWorker;
import eventaudittrail.workers.ValidateEventWorker;
import eventaudittrail.workers.LogValidatedWorker;
import eventaudittrail.workers.ProcessEventWorker;
import eventaudittrail.workers.LogProcessedWorker;
import eventaudittrail.workers.FinalizeAuditWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Audit Trail Demo
 *
 * Demonstrates a sequential audit trail workflow:
 *   at_log_received -> at_validate_event -> at_log_validated ->
 *   at_process_event -> at_log_processed -> at_finalize_audit
 *
 * Run:
 *   java -jar target/event-audit-trail-1.0.0.jar
 */
public class EventAuditTrailExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Audit Trail Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "at_log_received", "at_validate_event", "at_log_validated",
                "at_process_event", "at_log_processed", "at_finalize_audit"));
        System.out.println("  Registered: at_log_received, at_validate_event, at_log_validated, at_process_event, at_log_processed, at_finalize_audit\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_audit_trail_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LogReceivedWorker(),
                new ValidateEventWorker(),
                new LogValidatedWorker(),
                new ProcessEventWorker(),
                new LogProcessedWorker(),
                new FinalizeAuditWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_audit_trail_wf", 1,
                Map.of("eventId", "evt-audit-001",
                        "eventType", "order.created",
                        "eventData", Map.of(
                                "orderId", "ORD-9921",
                                "amount", 340.50,
                                "customer", "acme-corp")));
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
