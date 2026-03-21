package imageprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import imageprocessing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Image Processing Demo
 *
 * Demonstrates a FORK_JOIN workflow:
 *   ip_load_image -> FORK(ip_resize, ip_watermark, ip_optimize) -> JOIN -> ip_finalize
 */
public class ImageProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Image Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ip_load_image", "ip_resize", "ip_watermark", "ip_optimize", "ip_finalize"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadImageWorker(), new ResizeWorker(), new WatermarkWorker(),
                new OptimizeWorker(), new FinalizeWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...");
        String wfId = client.startWorkflow("image_processing", 1,
                Map.of("imageUrl", "https://example.com/photos/landscape-4k.png",
                        "sizes", List.of(Map.of("w", 1920, "h", 1080), Map.of("w", 800, "h", 450), Map.of("w", 150, "h", 150)),
                        "watermarkText", "Copyright 2024"));

        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
    }
}
