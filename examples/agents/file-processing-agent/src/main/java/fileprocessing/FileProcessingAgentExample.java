package fileprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import fileprocessing.workers.DetectFileTypeWorker;
import fileprocessing.workers.ExtractContentWorker;
import fileprocessing.workers.AnalyzeContentWorker;
import fileprocessing.workers.GenerateSummaryWorker;

import java.util.List;
import java.util.Map;

/**
 * File Processing Agent Demo
 *
 * Demonstrates a sequential pipeline of four workers that process an uploaded
 * file: detect type, extract content, analyze, and generate summary.
 *   detect_file_type -> extract_content -> analyze_content -> generate_summary
 *
 * Run:
 *   java -jar target/file-processing-agent-1.0.0.jar
 */
public class FileProcessingAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== File Processing Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "fp_detect_file_type", "fp_extract_content",
                "fp_analyze_content", "fp_generate_summary"));
        System.out.println("  Registered: fp_detect_file_type, fp_extract_content, fp_analyze_content, fp_generate_summary\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'file_processing_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectFileTypeWorker(),
                new ExtractContentWorker(),
                new AnalyzeContentWorker(),
                new GenerateSummaryWorker()
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
        String workflowId = client.startWorkflow("file_processing_agent", 1,
                Map.of("fileName", "acme_q4_2025_financial_report.pdf",
                        "fileSize", 245760,
                        "mimeType", "application/pdf"));
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
