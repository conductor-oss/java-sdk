package videotranscoding;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import videotranscoding.workers.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Video Transcoding Demo -- Parallel Multi-Resolution Transcoding
 *
 * Pattern:
 *   analyze -> FORK(720p, 1080p, 4K) -> JOIN -> package
 */
public class VideoTranscodingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Video Transcoding Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "vt_analyze_video", "vt_transcode_720p", "vt_transcode_1080p",
                "vt_transcode_4k", "vt_package_outputs"));
        System.out.println("  Registered.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalyzeVideoWorker(),
                new Transcode720pWorker(),
                new Transcode1080pWorker(),
                new Transcode4kWorker(),
                new PackageOutputsWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("videoUrl", "s3://media-bucket/raw/interview_4k.mp4");
        input.put("outputFormats", List.of("720p", "1080p", "4K"));

        String workflowId = client.startWorkflow("video_transcoding", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
