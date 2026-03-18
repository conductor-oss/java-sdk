package etlbasics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Loads validated records into a destination file (JSON format).
 * If destination is a file path, writes records to that file.
 * If destination is not a valid path, uses a temp file.
 * Input: records, destination
 * Output: loadedCount, destination (actual file path written)
 */
public class LoadDataWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String getTaskDefName() {
        return "el_load_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        String destination = (String) task.getInputData().get("destination");
        if (destination == null) {
            destination = "default-store";
        }

        TaskResult result = new TaskResult(task);

        try {
            // Write records to a real file
            File outputFile;
            if (destination.contains("/") || destination.contains("\\") || destination.endsWith(".json")) {
                outputFile = new File(destination);
                // Ensure parent directories exist
                if (outputFile.getParentFile() != null) {
                    outputFile.getParentFile().mkdirs();
                }
            } else {
                // Use a temp file with the destination name as prefix
                outputFile = File.createTempFile("etl_" + destination.replaceAll("[^a-zA-Z0-9]", "_") + "_", ".json");
                outputFile.deleteOnExit();
            }

            MAPPER.writeValue(outputFile, records);
            String actualPath = outputFile.getAbsolutePath();

            System.out.println("  [el_load_data] Loaded " + records.size() + " records to " + actualPath
                    + " (" + outputFile.length() + " bytes)");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("loadedCount", records.size());
            result.getOutputData().put("destination", actualPath);
        } catch (Exception e) {
            System.err.println("  [el_load_data] Error loading data: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Load failed: " + e.getMessage());
        }

        return result;
    }
}
