package dataqualitychecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks data accuracy using real validation rules:
 *   - Email format: validates with RFC-compliant regex pattern
 *   - Status: must be one of the allowed values
 *   - Phone format (if present): validates common phone patterns
 * Reports per-record accuracy issues.
 *
 * Input: records (list)
 * Output: score (double), accurateCount (int), issues (list of per-record issues)
 */
public class CheckAccuracyWorker implements Worker {

    private static final Set<String> VALID_STATUSES = Set.of("active", "inactive", "pending");

    // RFC 5322 simplified email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    // Common phone patterns: (xxx) xxx-xxxx, xxx-xxx-xxxx, +x-xxx-xxx-xxxx, etc.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{1,3}?[-.\\s]?\\(?[0-9]{1,4}\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}$");

    @Override
    public String getTaskDefName() {
        return "qc_check_accuracy";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        int accurate = 0;
        List<Map<String, Object>> issues = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            List<String> recordIssues = new ArrayList<>();

            // Validate email format
            String email = record.get("email") != null ? record.get("email").toString() : "";
            if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                recordIssues.add("invalid email format: " + email);
            } else if (email.isEmpty()) {
                recordIssues.add("missing email");
            }

            // Validate status
            String status = record.get("status") != null ? record.get("status").toString() : "";
            if (!status.isEmpty() && !VALID_STATUSES.contains(status)) {
                recordIssues.add("invalid status: " + status);
            } else if (status.isEmpty()) {
                recordIssues.add("missing status");
            }

            // Validate phone if present
            String phone = record.get("phone") != null ? record.get("phone").toString() : "";
            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                recordIssues.add("invalid phone format: " + phone);
            }

            if (recordIssues.isEmpty()) {
                accurate++;
            } else {
                Map<String, Object> issue = new LinkedHashMap<>();
                issue.put("recordIndex", i);
                Object id = record.get("id");
                if (id != null) issue.put("id", id);
                issue.put("issues", recordIssues);
                issues.add(issue);
            }
        }

        double score = records.size() > 0
                ? Math.round((double) accurate / records.size() * 100.0) / 100.0
                : 0.0;

        System.out.println("  [accuracy] " + accurate + "/" + records.size()
                + " records accurate -> score: " + Math.round(score * 100) + "%"
                + " (" + issues.size() + " records with issues)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("accurateCount", accurate);
        result.getOutputData().put("issues", issues);
        return result;
    }
}
