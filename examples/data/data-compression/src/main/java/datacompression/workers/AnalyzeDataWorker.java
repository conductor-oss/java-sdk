package datacompression.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Analyzes data to determine size and record count.
 * Input: records (list)
 * Output: records (pass-through), recordCount (int), sizeBytes (int)
 */
public class AnalyzeDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmp_analyze_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        // Estimate size as the JSON string length
        String jsonStr = records.toString();
        int sizeBytes = jsonStr.getBytes().length;

        System.out.println("  [analyze] " + records.size() + " records, " + sizeBytes + " bytes original size");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("recordCount", records.size());
        result.getOutputData().put("sizeBytes", sizeBytes);
        return result;
    }
}
