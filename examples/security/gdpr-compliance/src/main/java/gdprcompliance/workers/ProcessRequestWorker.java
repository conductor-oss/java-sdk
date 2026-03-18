package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes GDPR data requests. Real PII detection using regex patterns.
 */
public class ProcessRequestWorker implements Worker {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(\\+?1[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");

    @Override public String getTaskDefName() { return "gdpr_process_request"; }

    @Override public TaskResult execute(Task task) {
        String requestType = (String) task.getInputData().get("requestType");
        String data = (String) task.getInputData().get("data");
        if (requestType == null) requestType = "access";
        if (data == null) data = "";

        // Real PII detection
        List<Map<String, String>> piiFound = new ArrayList<>();
        findPII(data, EMAIL_PATTERN, "email", piiFound);
        findPII(data, SSN_PATTERN, "ssn", piiFound);
        findPII(data, PHONE_PATTERN, "phone", piiFound);
        findPII(data, CREDIT_CARD_PATTERN, "credit_card", piiFound);

        // Process based on request type
        String processedData = data;
        if ("erasure".equals(requestType) || "anonymize".equals(requestType)) {
            processedData = maskPII(data);
        }

        System.out.println("  [gdpr] " + requestType + ": found " + piiFound.size() + " PII elements");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("piiFound", piiFound);
        result.getOutputData().put("piiCount", piiFound.size());
        result.getOutputData().put("processedData", processedData);
        result.getOutputData().put("requestType", requestType);
        result.getOutputData().put("processedAt", Instant.now().toString());
        return result;
    }

    private void findPII(String data, Pattern pattern, String type, List<Map<String, String>> results) {
        Matcher m = pattern.matcher(data);
        while (m.find()) {
            results.add(Map.of("type", type, "value", maskValue(m.group()), "position", String.valueOf(m.start())));
        }
    }

    private String maskValue(String value) {
        if (value.length() <= 4) return "****";
        return value.substring(0, 2) + "*".repeat(value.length() - 4) + value.substring(value.length() - 2);
    }

    private String maskPII(String data) {
        String result = data;
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL_REDACTED]");
        result = SSN_PATTERN.matcher(result).replaceAll("[SSN_REDACTED]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE_REDACTED]");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[CC_REDACTED]");
        return result;
    }
}
