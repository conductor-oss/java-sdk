package sequentialtasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform phase of the ETL pipeline.
 * Takes raw data and a format, adds grade (A/B/C) and normalized score.
 */
public class TransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "seq_transform";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> rawData =
                (List<Map<String, Object>>) task.getInputData().get("rawData");
        String format = (String) task.getInputData().get("format");
        if (format == null || format.isBlank()) {
            format = "standard";
        }

        System.out.println("  [seq_transform] Transforming " + rawData.size()
                + " records (format: " + format + ")");

        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> record : rawData) {
            Map<String, Object> newRecord = new HashMap<>(record);
            int score = ((Number) record.get("score")).intValue();
            newRecord.put("grade", calculateGrade(score));
            newRecord.put("normalizedScore", Math.round(score / 100.0 * 1000.0) / 1000.0);
            newRecord.put("format", format);
            transformed.add(newRecord);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformedData", transformed);
        result.getOutputData().put("transformedCount", transformed.size());
        return result;
    }

    static String calculateGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        return "C";
    }
}
