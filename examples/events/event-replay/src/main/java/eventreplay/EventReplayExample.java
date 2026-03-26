package eventreplay;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventreplay.workers.LoadHistoryWorker;
import eventreplay.workers.FilterEventsWorker;
import eventreplay.workers.ReplayEventsWorker;
import eventreplay.workers.GenerateReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Replay Demo
 *
 * Demonstrates a sequential pipeline of four workers that replay failed events:
 * load history, filter events, replay events, and generate a report.
 *   ep_load_history -> ep_filter_events -> ep_replay_events -> ep_generate_report
 *
 * Run:
 *   java -jar target/event-replay-1.0.0.jar
 */
public class EventReplayExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Replay Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ep_load_history", "ep_filter_events",
                "ep_replay_events", "ep_generate_report"));
        System.out.println("  Registered: ep_load_history, ep_filter_events, ep_replay_events, ep_generate_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_replay_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadHistoryWorker(),
                new FilterEventsWorker(),
                new ReplayEventsWorker(),
                new GenerateReportWorker()
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
        String workflowId = client.startWorkflow("event_replay_wf", 1,
                Map.of("sourceStream", "order-events",
                        "startTime", "2026-03-07T08:00:00Z",
                        "endTime", "2026-03-07T12:00:00Z",
                        "filterCriteria", Map.of("status", "failed", "eventType", "order.created")));
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
