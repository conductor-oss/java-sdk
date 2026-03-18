package dataqualitychecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks data completeness by verifying required fields are present and non-empty.
 * Also reports which specific records and fields are missing.
 * Input: records (list)
 * Output: score (double), filledFields (int), totalFields (int), missingDetails (list)
 */
public class CheckCompletenessWorker implements Worker {

    private static final List<String> REQUIRED_FIELDS = List.of("id", "name", "email", "status");

    @Override
    public String getTaskDefName() {
        return "qc_check_completeness";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        int totalFields = 0;
        int filledFields = 0;
        List<Map<String, Object>> missingDetails = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            List<String> missingFields = new ArrayList<>();

            for (String field : REQUIRED_FIELDS) {
                totalFields++;
                Object value = record.get(field);
                if (value != null && !"".equals(value.toString().trim())) {
                    filledFields++;
                } else {
                    missingFields.add(field);
                }
            }

            if (!missingFields.isEmpty()) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("recordIndex", i);
                Object id = record.get("id");
                if (id != null) {
                    detail.put("id", id);
                }
                detail.put("missingFields", missingFields);
                missingDetails.add(detail);
            }
        }

        double score = totalFields > 0
                ? Math.round((double) filledFields / totalFields * 100.0) / 100.0
                : 0.0;

        System.out.println("  [completeness] " + filledFields + "/" + totalFields
                + " fields filled -> score: " + Math.round(score * 100) + "%"
                + " (" + missingDetails.size() + " records with missing data)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("filledFields", filledFields);
        result.getOutputData().put("totalFields", totalFields);
        result.getOutputData().put("missingDetails", missingDetails);
        return result;
    }
}
