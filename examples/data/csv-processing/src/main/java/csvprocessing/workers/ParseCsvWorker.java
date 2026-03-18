package csvprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a CSV string into structured rows.
 * Input: csvData (string), delimiter, hasHeader
 * Output: rows (list of maps), headers (list), rowCount
 */
public class ParseCsvWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cv_parse_csv";
    }

    @Override
    public TaskResult execute(Task task) {
        String csvData = (String) task.getInputData().get("csvData");
        if (csvData == null) {
            csvData = "";
        }

        String delimiter = (String) task.getInputData().get("delimiter");
        if (delimiter == null || delimiter.isEmpty()) {
            delimiter = ",";
        }

        String[] allLines = csvData.split("\n");
        List<String> lines = new ArrayList<>();
        for (String line : allLines) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }

        List<String> headers;
        List<Map<String, String>> rows = new ArrayList<>();

        if (lines.isEmpty()) {
            headers = List.of();
        } else {
            headers = Arrays.stream(lines.get(0).split(delimiter, -1))
                    .map(String::trim)
                    .toList();

            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(delimiter, -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    row.put(headers.get(j), j < values.length ? values[j].trim() : "");
                }
                rows.add(row);
            }
        }

        System.out.println("  [parse] Parsed " + rows.size() + " rows with "
                + headers.size() + " columns: " + headers);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rows", rows);
        result.getOutputData().put("headers", headers);
        result.getOutputData().put("rowCount", rows.size());
        return result;
    }
}
