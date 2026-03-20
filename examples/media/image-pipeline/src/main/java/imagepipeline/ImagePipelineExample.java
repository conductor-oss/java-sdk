package imagepipeline;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import imagepipeline.workers.UploadImageWorker;
import imagepipeline.workers.ResizeImageWorker;
import imagepipeline.workers.OptimizeImageWorker;
import imagepipeline.workers.WatermarkImageWorker;
import imagepipeline.workers.PushCdnWorker;
import java.util.List;
import java.util.Map;
public class ImagePipelineExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 513: Image Pipeline ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("imp_upload_image", "imp_resize_image", "imp_optimize_image", "imp_watermark_image", "imp_push_cdn"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new UploadImageWorker(), new ResizeImageWorker(), new OptimizeImageWorker(), new WatermarkImageWorker(), new PushCdnWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("image_pipeline_workflow", 1, Map.of("imageId", "IMG-513-001", "sourceUrl", "https://uploads.example.com/photos/513.jpg", "targetSizes", List.of("1200x900", "800x600", "400x300", "200x150"), "watermarkText", "Example Corp"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
