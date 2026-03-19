package pdfprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import pdfprocessing.workers.ExtractTextWorker;
import pdfprocessing.workers.ParseSectionsWorker;
import pdfprocessing.workers.AnalyzeContentWorker;
import pdfprocessing.workers.GenerateSummaryWorker;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * PDF Processing Demo
 *
 * Demonstrates a sequential data-processing workflow:
 *   pd_extract_text -> pd_parse_sections -> pd_analyze_content -> pd_generate_summary
 *
 * Run:
 *   java -jar target/pdf-processing-1.0.0.jar
 */
public class PdfProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== PDF Processing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pd_extract_text", "pd_parse_sections",
                "pd_analyze_content", "pd_generate_summary"));
        System.out.println("  Registered: pd_extract_text, pd_parse_sections, pd_analyze_content, pd_generate_summary\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'pdf_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExtractTextWorker(),
                new ParseSectionsWorker(),
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
        String workflowId = client.startWorkflow("pdf_processing", 1,
                Map.of("pdfBase64", generateSamplePdfBase64(),
                        "options", Map.of("extractImages", false, "ocrFallback", true)));
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

    /**
     * Generates a small sample PDF in-memory and returns it as a Base64 string.
     */
    private static String generateSamplePdfBase64() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset(50, 700);
                cs.showText("Data Strategy 2024 - Sample Document");
                cs.newLineAtOffset(0, -24);
                cs.setFont(font, 11);
                cs.showText("This is a sample PDF for the pdf-processing example.");
                cs.newLineAtOffset(0, -18);
                cs.showText("Section 1: Introduction to data governance and quality.");
                cs.newLineAtOffset(0, -18);
                cs.showText("Section 2: Cloud migration strategy and timeline.");
                cs.newLineAtOffset(0, -18);
                cs.showText("Section 3: Analytics platform modernization roadmap.");
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}
