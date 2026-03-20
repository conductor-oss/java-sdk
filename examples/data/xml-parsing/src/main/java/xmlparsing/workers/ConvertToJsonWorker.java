package xmlparsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts extracted data to JSON records by adding source and parsedAt metadata.
 * Input: extractedData (list of record maps)
 * Output: jsonRecords (list of enriched maps), count
 */
public class ConvertToJsonWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xp_convert_to_json";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> extractedData = (List<Map<String, Object>>) task.getInputData().get("extractedData");
        if (extractedData == null) {
            extractedData = List.of();
        }

        System.out.println("  [xp_convert_to_json] Converting " + extractedData.size() + " records to JSON");

        List<Map<String, Object>> jsonRecords = new ArrayList<>();
        for (Map<String, Object> record : extractedData) {
            Map<String, Object> jsonRecord = new LinkedHashMap<>(record);
            jsonRecord.put("source", "xml_feed");
            jsonRecord.put("parsedAt", "2026-01-15T10:00:00Z");
            jsonRecords.add(jsonRecord);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("jsonRecords", jsonRecords);
        result.getOutputData().put("count", jsonRecords.size());
        return result;
    }
}
