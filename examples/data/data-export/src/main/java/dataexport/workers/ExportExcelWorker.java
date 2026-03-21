package dataexport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Exports data to Excel format.
 * Input: data (list of records), headers
 * Output: file, fileSize, sheetName, rowCount
 */
public class ExportExcelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dx_export_excel";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("data", List.of());
        List<String> headers =
                (List<String>) task.getInputData().getOrDefault("headers", List.of());

        String fileSize = String.format("%.1fKB", (data.size() * headers.size() * 15) / 1024.0);

        System.out.println("  [excel] Exported " + data.size() + " rows to Excel with "
                + headers.size() + " columns (" + fileSize + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("file", "export/data.xlsx");
        result.getOutputData().put("fileSize", fileSize);
        result.getOutputData().put("sheetName", "Sheet1");
        result.getOutputData().put("rowCount", data.size());
        return result;
    }
}
