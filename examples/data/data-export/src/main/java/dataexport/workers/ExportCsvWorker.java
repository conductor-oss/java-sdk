package dataexport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports data to CSV format.
 * Input: data (list of records), headers
 * Output: file, fileSize, rowCount
 */
public class ExportCsvWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dx_export_csv";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("data", List.of());
        List<String> headers =
                (List<String>) task.getInputData().getOrDefault("headers", List.of());

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        for (Map<String, Object> row : data) {
            csv.append(headers.stream().map(h -> String.valueOf(row.getOrDefault(h, "")))
                    .collect(Collectors.joining(","))).append("\n");
        }
        String fileSize = String.format("%.1fKB", csv.length() / 1024.0);

        System.out.println("  [csv] Exported " + data.size() + " rows to CSV (" + fileSize + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("file", "export/data.csv");
        result.getOutputData().put("fileSize", fileSize);
        result.getOutputData().put("rowCount", data.size());
        return result;
    }
}
