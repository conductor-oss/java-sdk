package pdfprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractTextWorkerTest {

    private final ExtractTextWorker worker = new ExtractTextWorker();

    @TempDir
    static Path tempDir;

    static File testPdfFile;
    static String testPdfBase64;

    @BeforeAll
    static void createTestPdf() throws Exception {
        // Create a real PDF with 2 pages using PDFBox
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage();
            doc.addPage(page1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Chapter 1: Introduction");
                cs.newLineAtOffset(0, -20);
                cs.showText("This document covers data processing strategies.");
                cs.endText();
            }

            PDPage page2 = new PDPage();
            doc.addPage(page2);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Chapter 2: Architecture");
                cs.newLineAtOffset(0, -20);
                cs.showText("A well-designed data architecture enables scalability.");
                cs.endText();
            }

            testPdfFile = tempDir.resolve("test.pdf").toFile();
            doc.save(testPdfFile);

            // Also create base64 version
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            testPdfBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    @Test
    void taskDefName() {
        assertEquals("pd_extract_text", worker.getTaskDefName());
    }

    @Test
    void extractsTextFromFilePath() {
        Task task = taskWith(Map.of("pdfPath", testPdfFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String text = (String) result.getOutputData().get("text");
        assertNotNull(text);
        assertTrue(text.contains("Chapter 1"));
        assertTrue(text.contains("data processing"));
    }

    @Test
    void extractsTextFromBase64() {
        Task task = taskWith(Map.of("pdfBase64", testPdfBase64));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String text = (String) result.getOutputData().get("text");
        assertNotNull(text);
        assertTrue(text.contains("Chapter 2"));
        assertTrue(text.contains("architecture"));
    }

    @Test
    void returnsTwoPages() {
        Task task = taskWith(Map.of("pdfPath", testPdfFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("pageCount"));
    }

    @Test
    void charCountMatchesTextLength() {
        Task task = taskWith(Map.of("pdfPath", testPdfFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        String text = (String) result.getOutputData().get("text");
        int charCount = (int) result.getOutputData().get("charCount");
        assertEquals(text.length(), charCount);
    }

    @Test
    void failsForMissingFile() {
        Task task = taskWith(Map.of("pdfPath", "/nonexistent/path/missing.pdf"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("not found"));
    }

    @Test
    void failsWhenNoInputProvided() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("No PDF input"));
    }

    @Test
    void failsForNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("pdfPath", null);
        input.put("pdfBase64", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void legacyPdfUrlFieldWorksAsFilePath() {
        Task task = taskWith(Map.of("pdfUrl", testPdfFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String text = (String) result.getOutputData().get("text");
        assertNotNull(text);
        assertTrue(text.contains("Chapter 1"));
    }

    @Test
    void base64TakesPriorityOverPath() {
        // Both provided; base64 should be used
        Task task = taskWith(Map.of(
                "pdfPath", "/nonexistent/path.pdf",
                "pdfBase64", testPdfBase64));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("text"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
