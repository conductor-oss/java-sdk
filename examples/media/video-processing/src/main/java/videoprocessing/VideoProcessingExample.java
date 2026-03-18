package videoprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import videoprocessing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Video Processing Pipeline
 *
 * Demonstrates a workflow that:
 *   1. Uploads raw video to storage (vid_upload)
 *   2. Transcodes to multiple resolutions (vid_transcode)
 *   3. Generates thumbnails (vid_thumbnail)
 *   4. Extracts and indexes metadata (vid_metadata)
 *   5. Publishes the video (vid_publish)
 *
 * Run:
 *   java -jar target/video-processing-1.0.0.jar
 *   java -jar target/video-processing-1.0.0.jar --workers
 */
public class VideoProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Video Processing Pipeline ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("vid_upload", "vid_transcode", "vid_thumbnail", "vid_metadata", "vid_publish"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new UploadWorker(),
                new TranscodeWorker(),
                new ThumbnailWorker(),
                new MetadataWorker(),
                new PublishWorker()
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

        System.out.println("Step 4: Starting workflow...");
        String workflowId = client.startWorkflow("video_processing_workflow", 1, Map.of(
                "videoId", "VID-001",
                "sourceUrl", "https://uploads.example.com/raw/VID-001.mp4",
                "title", "Introduction to Conductor Workflows",
                "creatorId", "creator-42"
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
