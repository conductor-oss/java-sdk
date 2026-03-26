package livestreaming;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import livestreaming.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 522: Live Streaming Workflow
 *
 * Live stream management: setup stream, encode for distribution,
 * distribute to CDN, monitor quality, and archive recording.
 */
public class LiveStreamingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 522: Live Streaming ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "lsm_setup_stream", "lsm_encode_stream", "lsm_distribute_stream",
                "lsm_monitor_quality", "lsm_archive_stream"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SetupStreamWorker(),
                new EncodeStreamWorker(),
                new DistributeStreamWorker(),
                new MonitorQualityWorker(),
                new ArchiveStreamWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow(
                "live_streaming_workflow", 1,
                Map.of("streamId", "STREAM-522-001",
                        "channelId", "CH-TECH-42",
                        "title", "Conductor Workshop Live",
                        "resolution", "1080p"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
