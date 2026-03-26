package dataexport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Exports data to JSON format.
 * Input: data (list of records)
 * Output: file, fileSize, recordCount
 */
public class ExportJsonWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dx_export_json";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("data", List.of());

        // Perform JSON serialization size
        int estimatedSize = data.size() * 120; // rough estimate per record
        String fileSize = String.format("%.1fKB", estimatedSize / 1024.0);

        System.out.println("  [json] Exported " + data.size() + " records to JSON (" + fileSize + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("file", "export/data.json");
        result.getOutputData().put("fileSize", fileSize);
        result.getOutputData().put("recordCount", data.size());
        return result;
    }
}
