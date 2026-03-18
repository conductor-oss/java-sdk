package pdfprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;

/**
 * Extracts text from a real PDF document using Apache PDFBox.
 * Input: pdfPath (file path) OR pdfBase64 (base64-encoded PDF content)
 * Output: text, pageCount, charCount
 */
public class ExtractTextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pd_extract_text";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String pdfPath = (String) task.getInputData().get("pdfPath");
        String pdfBase64 = (String) task.getInputData().get("pdfBase64");
        // Legacy support: also accept "pdfUrl" as a file path
        if (pdfPath == null) {
            pdfPath = (String) task.getInputData().get("pdfUrl");
        }

        try {
            PDDocument document;

            if (pdfBase64 != null && !pdfBase64.isEmpty()) {
                byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
                document = Loader.loadPDF(pdfBytes);
            } else if (pdfPath != null && !pdfPath.isEmpty()) {
                File pdfFile = new File(pdfPath);
                if (!pdfFile.exists()) {
                    result.setStatus(TaskResult.Status.FAILED);
                    result.getOutputData().put("error", "PDF file not found: " + pdfPath);
                    return result;
                }
                document = Loader.loadPDF(pdfFile);
            } else {
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", "No PDF input provided. Supply pdfPath or pdfBase64.");
                return result;
            }

            try (document) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                int pageCount = document.getNumberOfPages();

                System.out.println("  [extract] Extracted text from PDF -- " + pageCount
                        + " pages, " + text.length() + " chars");

                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("text", text);
                result.getOutputData().put("pageCount", pageCount);
                result.getOutputData().put("charCount", text.length());
            }
        } catch (Exception e) {
            System.err.println("  [extract] Error extracting PDF: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "PDF extraction failed: " + e.getMessage());
        }

        return result;
    }
}
