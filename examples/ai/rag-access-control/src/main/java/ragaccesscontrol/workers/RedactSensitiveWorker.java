package ragaccesscontrol.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Worker that redacts sensitive information from documents based on clearance level.
 * Redacts SSN patterns and salary figures unless the user has top-secret clearance.
 */
public class RedactSensitiveWorker implements Worker {

    private static final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private static final Pattern SALARY_PATTERN = Pattern.compile("\\$[\\d,]+(?:\\.\\d{2})?");

    @Override
    public String getTaskDefName() {
        return "ac_redact_sensitive";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> documents = (List<Map<String, Object>>) task.getInputData().get("documents");
        String clearanceLevel = (String) task.getInputData().get("clearanceLevel");

        boolean skipRedaction = "top-secret".equals(clearanceLevel);

        List<Map<String, Object>> redactedDocuments = new ArrayList<>();
        int fieldsRedacted = 0;

        for (Map<String, Object> doc : documents) {
            Map<String, Object> redactedDoc = new HashMap<>(doc);
            String content = (String) doc.get("content");

            if (!skipRedaction) {
                String redactedContent = content;

                if (SSN_PATTERN.matcher(redactedContent).find()) {
                    redactedContent = SSN_PATTERN.matcher(redactedContent).replaceAll("[SSN REDACTED]");
                    fieldsRedacted++;
                }

                if (SALARY_PATTERN.matcher(redactedContent).find()) {
                    redactedContent = SALARY_PATTERN.matcher(redactedContent).replaceAll("[SALARY REDACTED]");
                    fieldsRedacted++;
                }

                redactedDoc.put("content", redactedContent);
            }

            redactedDocuments.add(redactedDoc);
        }

        System.out.println("  [redact] Processed " + documents.size() + " documents, redacted "
                + fieldsRedacted + " sensitive fields (clearance: " + clearanceLevel + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("redactedDocuments", redactedDocuments);
        result.getOutputData().put("fieldsRedacted", fieldsRedacted);
        return result;
    }
}
