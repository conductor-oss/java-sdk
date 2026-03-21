package fileprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Detects the file type from the file name extension and returns
 * the file type, extraction method, and category.
 */
public class DetectFileTypeWorker implements Worker {

    private static final Map<String, String[]> EXTENSION_MAP = Map.of(
            "pdf",  new String[]{"document",     "pdf_parser",  "text_document"},
            "docx", new String[]{"document",     "docx_parser", "text_document"},
            "csv",  new String[]{"spreadsheet",  "csv_parser",  "structured_data"},
            "xlsx", new String[]{"spreadsheet",  "xlsx_parser", "structured_data"},
            "png",  new String[]{"image",        "ocr",         "visual"},
            "jpg",  new String[]{"image",        "ocr",         "visual"},
            "json", new String[]{"data",         "json_parser", "structured_data"}
    );

    @Override
    public String getTaskDefName() {
        return "fp_detect_file_type";
    }

    @Override
    public TaskResult execute(Task task) {
        String fileName = (String) task.getInputData().get("fileName");
        if (fileName == null || fileName.isBlank()) {
            fileName = "unknown.bin";
        }

        String mimeType = (String) task.getInputData().get("mimeType");
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        System.out.println("  [fp_detect_file_type] Detecting file type for: " + fileName);

        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        String[] mapping = EXTENSION_MAP.getOrDefault(extension,
                new String[]{"unknown", "raw_text", "other"});

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fileType", mapping[0]);
        result.getOutputData().put("extractionMethod", mapping[1]);
        result.getOutputData().put("category", mapping[2]);
        result.getOutputData().put("extension", extension);
        result.getOutputData().put("originalMimeType", mimeType);
        return result;
    }
}
