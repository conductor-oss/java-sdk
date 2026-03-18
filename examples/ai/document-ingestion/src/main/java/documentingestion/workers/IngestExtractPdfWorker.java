package documentingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Extracts text from PDF documents using Apache PDFBox.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>Local PDF files</b> — reads and parses with PDFBox</li>
 *   <li><b>Local text files</b> — reads directly as text</li>
 *   <li><b>HTTP/HTTPS URLs</b> — downloads and parses PDFs, or reads text content</li>
 * </ul>
 *
 * <p>Returns extracted text, page count, and character count.
 */
public class IngestExtractPdfWorker implements Worker {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String getTaskDefName() {
        return "ingest_extract_pdf";
    }

    @Override
    public TaskResult execute(Task task) {
        String documentUrl = (String) task.getInputData().get("documentUrl");

        System.out.println("  [extract] Processing document: " + documentUrl);

        TaskResult result = new TaskResult(task);

        if (documentUrl == null || documentUrl.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("No documentUrl provided");
            return result;
        }

        try {
            ExtractionResult extracted = extractText(documentUrl);

            System.out.println("  [extract] Extracted " + extracted.charCount + " chars from "
                    + extracted.pageCount + " pages");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("text", extracted.text);
            result.getOutputData().put("pageCount", extracted.pageCount);
            result.getOutputData().put("charCount", extracted.charCount);
            return result;

        } catch (Exception e) {
            System.err.println("  [extract] Failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Text extraction failed: " + e.getMessage());
            return result;
        }
    }

    private ExtractionResult extractText(String source) throws Exception {
        // Try as local file
        try {
            Path filePath = Path.of(source);
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                if (source.toLowerCase().endsWith(".pdf")) {
                    return extractFromPdf(Files.newInputStream(filePath));
                } else {
                    String text = Files.readString(filePath);
                    int pages = Math.max(1, text.length() / 2000);
                    return new ExtractionResult(text, pages, text.length());
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Not a valid path
        }

        // Try as HTTP URL
        if (source.startsWith("http://") || source.startsWith("https://")) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(source))
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();

            if (source.toLowerCase().endsWith(".pdf")) {
                HttpResponse<InputStream> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return extractFromPdf(response.body());
                }
                throw new IOException("HTTP " + response.statusCode() + " fetching PDF: " + source);
            } else {
                HttpResponse<String> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String text = response.body();
                    int pages = Math.max(1, text.length() / 2000);
                    return new ExtractionResult(text, pages, text.length());
                }
                throw new IOException("HTTP " + response.statusCode() + " fetching: " + source);
            }
        }

        throw new IOException("Could not read document from: " + source);
    }

    private ExtractionResult extractFromPdf(InputStream pdfStream) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            int pageCount = doc.getNumberOfPages();
            return new ExtractionResult(text, pageCount, text.length());
        }
    }

    private record ExtractionResult(String text, int pageCount, int charCount) {}
}
