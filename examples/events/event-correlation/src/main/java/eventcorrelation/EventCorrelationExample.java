package eventcorrelation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventcorrelation.workers.InitCorrelationWorker;
import eventcorrelation.workers.ReceiveOrderWorker;
import eventcorrelation.workers.ReceivePaymentWorker;
import eventcorrelation.workers.ReceiveShippingWorker;
import eventcorrelation.workers.CorrelateEventsWorker;
import eventcorrelation.workers.ProcessCorrelatedWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Correlation Demo — FORK_JOIN Pattern
 *
 * Demonstrates event correlation using Conductor's FORK_JOIN:
 * 1. Initialize a correlation session
 * 2. Fork to receive order, payment, and shipping events in parallel
 * 3. Join all events
 * 4. Correlate the events together
 * 5. Process the correlated data
 *
 * Run:
 *   java -jar target/event-correlation-1.0.0.jar
 */
public class EventCorrelationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Correlation Demo: FORK_JOIN Pattern ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ec_init_correlation", "ec_receive_order", "ec_receive_payment",
                "ec_receive_shipping", "ec_correlate_events", "ec_process_correlated"));
        System.out.println("  Registered: ec_init_correlation, ec_receive_order, ec_receive_payment, "
                + "ec_receive_shipping, ec_correlate_events, ec_process_correlated\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_correlation_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new InitCorrelationWorker(),
                new ReceiveOrderWorker(),
                new ReceivePaymentWorker(),
                new ReceiveShippingWorker(),
                new CorrelateEventsWorker(),
                new ProcessCorrelatedWorker()
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
        String workflowId = client.startWorkflow("event_correlation_wf", 1,
                Map.of("correlationId", "corr-fixed-001", "expectedEvents", 3));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
