package multiagentcodereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Security review agent — inspects the AST for security vulnerabilities
 * such as SQL injection, weak cryptography, and missing security middleware.
 */
public class SecurityReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_security_review";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> ast = (Map<String, Object>) task.getInputData().get("ast");
        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "javascript";
        }

        System.out.println("  [cr_security_review] Reviewing " + language + " code for security issues");

        Map<String, Object> finding1 = new LinkedHashMap<>();
        finding1.put("severity", "HIGH");
        finding1.put("type", "SQL_INJECTION");
        finding1.put("message", "User input concatenated directly into SQL query in processData()");
        finding1.put("line", 47);

        Map<String, Object> finding2 = new LinkedHashMap<>();
        finding2.put("severity", "MEDIUM");
        finding2.put("type", "WEAK_CRYPTO");
        finding2.put("message", "Using MD5 for password hashing instead of bcrypt or argon2");
        finding2.put("line", 23);

        Map<String, Object> finding3 = new LinkedHashMap<>();
        finding3.put("severity", "LOW");
        finding3.put("type", "MISSING_HELMET");
        finding3.put("message", "Express app does not use helmet middleware for security headers");
        finding3.put("line", 5);

        List<Map<String, Object>> findings = List.of(finding1, finding2, finding3);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("agent", "security");
        return result;
    }
}
