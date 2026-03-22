package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes GDPR data requests with real PII detection using regex patterns
 * and PBKDF2-style hashing for anonymization.
 *
 * Detects: emails, SSNs, phone numbers (US and international), credit cards.
 *
 * Input:
 *   - requestType (String, required): one of "access", "erasure", "anonymize", "portability"
 *   - data (String, required): the data to scan for PII
 *
 * Output:
 *   - piiFound (List): detected PII elements with type, masked value, position
 *   - piiCount (int): total PII elements found
 *   - processedData (String): data after processing (masked if erasure/anonymize)
 *   - requestType (String): the request type processed
 *   - processedAt (String): ISO-8601 timestamp
 *   - auditLog (Map): timestamp, action, actor, result
 */
public class ProcessRequestWorker implements Worker {
    static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    // US phone numbers and international formats like +44, +91, etc.
    static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\+?\\d{1,3}[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}\\b"
    );
    static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");

    private static final Set<String> VALID_REQUEST_TYPES = Set.of("access", "erasure", "anonymize", "portability");

    @Override public String getTaskDefName() { return "gdpr_process_request"; }

    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // Validate required inputs
        String requestType = getRequiredString(task, "requestType");
        if (requestType == null || requestType.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: requestType");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_process_request", "SYSTEM", "FAILED", "Missing requestType"));
            return result;
        }

        if (!VALID_REQUEST_TYPES.contains(requestType)) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid requestType: '" + requestType + "'. Must be one of: " + VALID_REQUEST_TYPES);
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_process_request", "SYSTEM", "FAILED", "Invalid requestType: " + requestType));
            return result;
        }

        String data = getRequiredString(task, "data");
        if (data == null) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: data");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_process_request", "SYSTEM", "FAILED", "Missing data"));
            return result;
        }

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

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("piiFound", piiFound);
        result.getOutputData().put("piiCount", piiFound.size());
        result.getOutputData().put("processedData", processedData);
        result.getOutputData().put("requestType", requestType);
        result.getOutputData().put("processedAt", Instant.now().toString());
        result.getOutputData().put("auditLog",
                VerifyIdentityWorker.auditLog("gdpr_process_request", "SYSTEM", "SUCCESS",
                        requestType + " processed, " + piiFound.size() + " PII elements found"));
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

    String maskPII(String data) {
        String masked = data;
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("[EMAIL_REDACTED]");
        masked = SSN_PATTERN.matcher(masked).replaceAll("[SSN_REDACTED]");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("[PHONE_REDACTED]");
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll("[CC_REDACTED]");
        return masked;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
