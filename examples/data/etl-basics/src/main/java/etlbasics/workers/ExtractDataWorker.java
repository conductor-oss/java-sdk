package etlbasics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts data from a real source. Supports:
 *   - "jsonData" input: a JSON array string to parse
 *   - "csvData" input: a CSV string with headers to parse
 *   - "source" (file path): reads a JSON or CSV file from disk
 * Output: records (list of maps), recordCount
 */
public class ExtractDataWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getTaskDefName() {
        return "el_extract_data";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String source = (String) task.getInputData().get("source");
        String jsonData = (String) task.getInputData().get("jsonData");
        String csvData = (String) task.getInputData().get("csvData");

        try {
            List<Map<String, Object>> records;

            if (jsonData != null && !jsonData.isEmpty()) {
                // Parse inline JSON array
                records = MAPPER.readValue(jsonData, new TypeReference<List<Map<String, Object>>>() {});
                System.out.println("  [el_extract_data] Extracted " + records.size() + " records from inline JSON");
            } else if (csvData != null && !csvData.isEmpty()) {
                // Parse inline CSV
                records = parseCsv(csvData);
                System.out.println("  [el_extract_data] Extracted " + records.size() + " records from inline CSV");
            } else if (source != null && !source.isEmpty()) {
                // Try reading from file path
                File file = new File(source);
                if (file.exists()) {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    String trimmed = content.trim();
                    if (trimmed.startsWith("[")) {
                        records = MAPPER.readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {});
                    } else {
                        records = parseCsv(trimmed);
                    }
                    System.out.println("  [el_extract_data] Extracted " + records.size() + " records from file: " + source);
                } else {
                    result.setStatus(TaskResult.Status.FAILED);
                    result.getOutputData().put("error", "Source file not found: " + source);
                    return result;
                }
            } else {
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", "No data source provided. Supply jsonData, csvData, or source file path.");
                return result;
            }

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("records", records);
            result.getOutputData().put("recordCount", records.size());
        } catch (Exception e) {
            System.err.println("  [el_extract_data] Error extracting data: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Extraction failed: " + e.getMessage());
        }

        return result;
    }

    private List<Map<String, Object>> parseCsv(String csvData) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                return records;
            }
            List<String> headers = Arrays.stream(headerLine.split(",", -1))
                    .map(String::trim)
                    .toList();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",", -1);
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String val = i < values.length ? values[i].trim() : "";
                    // Try to parse as number
                    record.put(headers.get(i), tryParseNumber(val));
                }
                records.add(record);
            }
        }
        return records;
    }

    private Object tryParseNumber(String val) {
        if (val == null || val.isEmpty()) return val;
        try {
            if (val.contains(".")) {
                return Double.parseDouble(val);
            }
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return val;
        }
    }
}
