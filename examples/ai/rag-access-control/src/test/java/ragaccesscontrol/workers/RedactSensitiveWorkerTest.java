package ragaccesscontrol.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedactSensitiveWorkerTest {

    private final RedactSensitiveWorker worker = new RedactSensitiveWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_redact_sensitive", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void redactsSsnAndSalary() {
        List<Map<String, Object>> docs = List.of(
                Map.of("id", "doc1", "content", "Safe content here.", "collection", "public-docs"),
                Map.of("id", "doc3", "content", "Average salary range: $95,000-$145,000. SSN: 123-45-6789.",
                        "collection", "hr-policies")
        );
        Task task = taskWith(Map.of(
                "documents", docs,
                "clearanceLevel", "confidential"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> redacted = (List<Map<String, Object>>) result.getOutputData().get("redactedDocuments");
        assertNotNull(redacted);
        assertEquals(2, redacted.size());

        String safeContent = (String) redacted.get(0).get("content");
        assertEquals("Safe content here.", safeContent);

        String redactedContent = (String) redacted.get(1).get("content");
        assertTrue(redactedContent.contains("[SSN REDACTED]"));
        assertTrue(redactedContent.contains("[SALARY REDACTED]"));
        assertFalse(redactedContent.contains("123-45-6789"));
        assertFalse(redactedContent.contains("$95,000"));

        int fieldsRedacted = (int) result.getOutputData().get("fieldsRedacted");
        assertTrue(fieldsRedacted > 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void topSecretSkipsRedaction() {
        List<Map<String, Object>> docs = List.of(
                Map.of("id", "doc4", "content", "CEO salary $450,000. SSN: 123-45-6789.",
                        "collection", "finance-reports")
        );
        Task task = taskWith(Map.of(
                "documents", docs,
                "clearanceLevel", "top-secret"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> redacted = (List<Map<String, Object>>) result.getOutputData().get("redactedDocuments");
        String content = (String) redacted.get(0).get("content");
        assertTrue(content.contains("$450,000"));
        assertTrue(content.contains("123-45-6789"));

        assertEquals(0, result.getOutputData().get("fieldsRedacted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
