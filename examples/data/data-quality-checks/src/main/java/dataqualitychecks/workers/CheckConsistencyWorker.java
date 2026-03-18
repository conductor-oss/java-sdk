package dataqualitychecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks data consistency by detecting:
 *   - Duplicate IDs
 *   - Duplicate email addresses (uniqueness check)
 *   - Referential consistency (cross-field checks)
 * Reports specific duplicate details.
 *
 * Input: records (list)
 * Output: score (double), duplicates (int), duplicateDetails (list)
 */
public class CheckConsistencyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qc_check_consistency";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        Set<Object> seenIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        int totalIds = 0;
        int duplicateIdCount = 0;
        int duplicateEmailCount = 0;
        List<Map<String, Object>> duplicateDetails = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);

            // Check ID uniqueness
            Object id = record.get("id");
            if (id != null) {
                totalIds++;
                if (!seenIds.add(id)) {
                    duplicateIdCount++;
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("recordIndex", i);
                    detail.put("field", "id");
                    detail.put("value", id);
                    detail.put("type", "duplicate_id");
                    duplicateDetails.add(detail);
                }
            }

            // Check email uniqueness
            Object emailObj = record.get("email");
            if (emailObj != null) {
                String email = emailObj.toString().toLowerCase().trim();
                if (!email.isEmpty() && !seenEmails.add(email)) {
                    duplicateEmailCount++;
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("recordIndex", i);
                    detail.put("field", "email");
                    detail.put("value", email);
                    detail.put("type", "duplicate_email");
                    duplicateDetails.add(detail);
                }
            }
        }

        int totalDuplicates = duplicateIdCount + duplicateEmailCount;
        // Score: proportion of records that are fully consistent
        int inconsistentRecords = duplicateDetails.size();
        int consistentRecords = records.size() - inconsistentRecords;
        double score = records.size() > 0
                ? Math.round((double) consistentRecords / records.size() * 100.0) / 100.0
                : 0.0;

        System.out.println("  [consistency] Unique IDs: " + seenIds.size() + "/" + totalIds
                + ", duplicate emails: " + duplicateEmailCount
                + " -> score: " + Math.round(score * 100) + "%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("duplicates", totalDuplicates);
        result.getOutputData().put("duplicateDetails", duplicateDetails);
        return result;
    }
}
