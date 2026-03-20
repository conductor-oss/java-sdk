package jsontransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapFieldsWorkerTest {

    private final MapFieldsWorker worker = new MapFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("jt_map_fields", worker.getTaskDefName());
    }

    @Test
    void mapsAllFieldsCorrectly() {
        Task task = taskWith(Map.of(
                "data", Map.of(
                        "cust_id", "C-9001",
                        "first_name", "Jane",
                        "last_name", "Doe",
                        "email", "JANE.DOE@EXAMPLE.COM",
                        "phone", "+1-555-0199",
                        "reg_date", "2024-01-15",
                        "acct_type", "premium")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mapped");
        assertEquals("C-9001", mapped.get("customerId"));
        assertEquals("Jane Doe", mapped.get("fullName"));
        assertEquals("jane.doe@example.com", mapped.get("emailAddress"));
        assertEquals("+1-555-0199", mapped.get("phoneNumber"));
        assertEquals("2024-01-15", mapped.get("registrationDate"));
        assertEquals("PREMIUM", mapped.get("accountType"));
    }

    @Test
    void mappedFieldCountIsSix() {
        Task task = taskWith(Map.of(
                "data", Map.of(
                        "cust_id", "C-100",
                        "first_name", "Bob",
                        "last_name", "Smith",
                        "email", "bob@test.com",
                        "phone", "555-1234",
                        "reg_date", "2024-06-01",
                        "acct_type", "basic")));
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("mappedFieldCount"));
    }

    @Test
    void emailIsLowercased() {
        Task task = taskWith(Map.of(
                "data", Map.of("email", "UPPER.CASE@EMAIL.COM")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mapped");
        assertEquals("upper.case@email.com", mapped.get("emailAddress"));
    }

    @Test
    void accountTypeIsUppercased() {
        Task task = taskWith(Map.of(
                "data", Map.of("acct_type", "premium")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mapped");
        assertEquals("PREMIUM", mapped.get("accountType"));
    }

    @Test
    void fullNameConcatenatesFirstAndLast() {
        Task task = taskWith(Map.of(
                "data", Map.of("first_name", "Alice", "last_name", "Wonder")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mapped");
        assertEquals("Alice Wonder", mapped.get("fullName"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mapped");
        assertEquals("", mapped.get("fullName"));
        assertEquals("", mapped.get("emailAddress"));
        assertEquals("STANDARD", mapped.get("accountType"));
    }

    @Test
    void handlesMissingDataKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("mappedFieldCount"));
    }

    @Test
    void handlesPartialData() {
        Task task = taskWith(Map.of(
                "data", Map.of("cust_id", "C-50", "email", "Partial@Test.com")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mapped");
        assertEquals("C-50", mapped.get("customerId"));
        assertEquals("partial@test.com", mapped.get("emailAddress"));
        assertEquals("", mapped.get("fullName"));
        assertNull(mapped.get("phoneNumber"));
        assertNull(mapped.get("registrationDate"));
        assertEquals("STANDARD", mapped.get("accountType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
