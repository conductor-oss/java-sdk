package dataencryption.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdentifyFieldsWorkerTest {

    private final IdentifyFieldsWorker worker = new IdentifyFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("dn_identify_fields", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void findsSensitiveFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "ssn", "123-45-6789", "creditCard", "4111"));
        Task task = taskWith(Map.of("records", records, "sensitiveFields", List.of("ssn", "creditCard")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> fields = (List<String>) result.getOutputData().get("fieldsToEncrypt");
        assertEquals(2, fields.size());
        assertTrue(fields.contains("ssn"));
        assertTrue(fields.contains("creditCard"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void onlyFindsExistingFields() {
        List<Map<String, Object>> records = List.of(Map.of("name", "Alice", "ssn", "123"));
        Task task = taskWith(Map.of("records", records, "sensitiveFields", List.of("ssn", "creditCard")));
        TaskResult result = worker.execute(task);

        List<String> fields = (List<String>) result.getOutputData().get("fieldsToEncrypt");
        assertEquals(1, fields.size());
        assertTrue(fields.contains("ssn"));
    }

    @Test
    void passesRecordsThrough() {
        List<Map<String, Object>> records = List.of(Map.of("name", "Bob"));
        Task task = taskWith(Map.of("records", records, "sensitiveFields", List.of("ssn")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "sensitiveFields", List.of("ssn")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("sensitiveFields", List.of("ssn"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
