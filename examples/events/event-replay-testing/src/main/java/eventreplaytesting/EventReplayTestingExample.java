package eventreplaytesting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventreplaytesting.workers.LoadEventsWorker;
import eventreplaytesting.workers.SetupSandboxWorker;
import eventreplaytesting.workers.ReplayEventWorker;
import eventreplaytesting.workers.CompareResultWorker;
import eventreplaytesting.workers.TestReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Replay Testing Demo
 *
 * Demonstrates a DO_WHILE-based event replay testing workflow:
 *   rt_load_events -> rt_setup_sandbox -> DO_WHILE(rt_replay_event -> rt_compare_result, 3 iters) -> rt_test_report
 *
 * Run:
 *   java -jar target/event-replay-testing-1.0.0.jar
 */
public class EventReplayTestingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Replay Testing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rt_load_events", "rt_setup_sandbox",
                "rt_replay_event", "rt_compare_result", "rt_test_report"));
        System.out.println("  Registered: rt_load_events, rt_setup_sandbox, rt_replay_event, rt_compare_result, rt_test_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_replay_testing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadEventsWorker(),
                new SetupSandboxWorker(),
                new ReplayEventWorker(),
                new CompareResultWorker(),
                new TestReportWorker()
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
        String workflowId = client.startWorkflow("event_replay_testing", 1,
                Map.of("testSuiteId", "suite-fixed-001",
                        "eventSource", "recorded-events-db"));
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
