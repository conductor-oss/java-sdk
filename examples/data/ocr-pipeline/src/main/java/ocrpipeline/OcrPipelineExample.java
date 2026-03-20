package ocrpipeline;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ocrpipeline.workers.PreprocessImageWorker;
import ocrpipeline.workers.ExtractTextWorker;
import ocrpipeline.workers.ValidateTextWorker;
import ocrpipeline.workers.StructureOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * OCR Pipeline Workflow Demo
 *
 * Demonstrates an OCR pipeline:
 *   oc_preprocess_image -> oc_extract_text -> oc_validate_text -> oc_structure_output
 *
 * Run:
 *   java -jar target/ocr-pipeline-1.0.0.jar
 */
public class OcrPipelineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== OCR Pipeline Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "oc_preprocess_image", "oc_extract_text",
                "oc_validate_text", "oc_structure_output"));
        System.out.println("  Registered: oc_preprocess_image, oc_extract_text, oc_validate_text, oc_structure_output\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'ocr_pipeline'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PreprocessImageWorker(),
                new ExtractTextWorker(),
                new ValidateTextWorker(),
                new StructureOutputWorker()
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
        String workflowId = client.startWorkflow("ocr_pipeline", 1,
                Map.of("imageUrl", "s3://documents/scans/invoice-0892.tiff",
                        "documentType", "invoice",
                        "language", "en"));
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
