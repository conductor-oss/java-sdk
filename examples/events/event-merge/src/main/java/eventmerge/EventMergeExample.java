package eventmerge;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventmerge.workers.CollectStreamAWorker;
import eventmerge.workers.CollectStreamBWorker;
import eventmerge.workers.CollectStreamCWorker;
import eventmerge.workers.MergeStreamsWorker;
import eventmerge.workers.ProcessMergedWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Merge Workflow Demo
 *
 * Demonstrates a FORK_JOIN workflow that collects events from three parallel
 * streams, merges them, and processes the combined result:
 *   FORK(mg_collect_stream_a, mg_collect_stream_b, mg_collect_stream_c)
 *     -> JOIN -> mg_merge_streams -> mg_process_merged
 *
 * Run:
 *   java -jar target/event-merge-1.0.0.jar
 */
public class EventMergeExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Merge Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mg_collect_stream_a", "mg_collect_stream_b", "mg_collect_stream_c",
                "mg_merge_streams", "mg_process_merged"));
        System.out.println("  Registered: mg_collect_stream_a, mg_collect_stream_b, mg_collect_stream_c, mg_merge_streams, mg_process_merged\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_merge_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectStreamAWorker(),
                new CollectStreamBWorker(),
                new CollectStreamCWorker(),
                new MergeStreamsWorker(),
                new ProcessMergedWorker()
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
        String workflowId = client.startWorkflow("event_merge_wf", 1,
                Map.of("sourceA", "api",
                        "sourceB", "mobile",
                        "sourceC", "iot"));
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
