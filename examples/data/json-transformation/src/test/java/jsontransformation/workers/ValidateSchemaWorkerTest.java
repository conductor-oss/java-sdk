package jsontransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateSchemaWorkerTest {

    private final ValidateSchemaWorker worker = new ValidateSchemaWorker();

    @Test
    void taskDefName() {
        assertEquals("jt_validate_schema", worker.getTaskDefName());
    }

    @Test
    void validRecordWithIdAndEmail() {
        Task task = taskWith(Map.of(
                "record", Map.of(
                        "identity", Map.of("id", "C-9001", "name", "Jane Doe"),
                        "contact", Map.of("email", "jane@example.com", "phone", "555-0199"),
                        "account", Map.of("type", "PREMIUM", "registeredAt", "2024-01-15"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("isValid"));
        assertNotNull(result.getOutputData().get("validated"));
    }

    @Test
    void invalidWhenMissingId() {
        Map<String, Object> identity = new HashMap<>();
        identity.put("id", null);
        identity.put("name", "Jane Doe");
        Task task = taskWith(Map.of(
                "record", Map.of(
                        "identity", identity,
                        "contact", Map.of("email", "jane@example.com"))));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void invalidWhenMissingEmail() {
        Map<String, Object> contact = new HashMap<>();
        contact.put("email", null);
        contact.put("phone", "555-0199");
        Task task = taskWith(Map.of(
                "record", Map.of(
                        "identity", Map.of("id", "C-100"),
                        "contact", contact)));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void invalidWhenEmptyId() {
        Task task = taskWith(Map.of(
                "record", Map.of(
                        "identity", Map.of("id", "", "name", "Test"),
                        "contact", Map.of("email", "test@test.com"))));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void invalidWhenEmptyEmail() {
        Task task = taskWith(Map.of(
                "record", Map.of(
                        "identity", Map.of("id", "C-100"),
                        "contact", Map.of("email", ""))));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void handlesNullRecord() {
        Map<String, Object> input = new HashMap<>();
        input.put("record", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void handlesMissingRecordKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void validatedOutputMatchesInputRecord() {
        Map<String, Object> record = Map.of(
                "identity", Map.of("id", "C-200", "name", "Test User"),
                "contact", Map.of("email", "test@example.com"),
                "account", Map.of("type", "BASIC"));
        Task task = taskWith(Map.of("record", record));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> validated = (Map<String, Object>) result.getOutputData().get("validated");
        @SuppressWarnings("unchecked")
        Map<String, Object> identity = (Map<String, Object>) validated.get("identity");
        assertEquals("C-200", identity.get("id"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
