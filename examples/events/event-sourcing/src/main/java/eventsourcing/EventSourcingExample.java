package eventsourcing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventsourcing.workers.LoadEventLogWorker;
import eventsourcing.workers.AppendNewEventWorker;
import eventsourcing.workers.RebuildStateWorker;
import eventsourcing.workers.SnapshotStateWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Sourcing Demo
 *
 * Demonstrates a sequential pipeline of four workers that implement event sourcing
 * for a bank account aggregate: load event log, append new event, rebuild state,
 * and snapshot state.
 *   ev_load_event_log -> ev_append_new_event -> ev_rebuild_state -> ev_snapshot_state
 *
 * Run:
 *   java -jar target/event-sourcing-1.0.0.jar
 */
public class EventSourcingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Sourcing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ev_load_event_log", "ev_append_new_event",
                "ev_rebuild_state", "ev_snapshot_state"));
        System.out.println("  Registered: ev_load_event_log, ev_append_new_event, ev_rebuild_state, ev_snapshot_state\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_sourcing_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadEventLogWorker(),
                new AppendNewEventWorker(),
                new RebuildStateWorker(),
                new SnapshotStateWorker()
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
        String workflowId = client.startWorkflow("event_sourcing_wf", 1,
                Map.of("aggregateId", "acct-1001",
                        "aggregateType", "BankAccount",
                        "newEvent", Map.of(
                                "type", "FundsDeposited",
                                "data", Map.of("amount", 250, "source", "check_deposit"))));
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
