package jsontransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseInputWorkerTest {

    private final ParseInputWorker worker = new ParseInputWorker();

    @Test
    void taskDefName() {
        assertEquals("jt_parse_input", worker.getTaskDefName());
    }

    @Test
    void parsesSourceJsonWithAllFields() {
        Task task = taskWith(Map.of(
                "sourceJson", Map.of(
                        "cust_id", "C-9001",
                        "first_name", "Jane",
                        "last_name", "Doe",
                        "email", "JANE.DOE@EXAMPLE.COM",
                        "phone", "+1-555-0199",
                        "reg_date", "2024-01-15",
                        "acct_type", "premium")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(7, result.getOutputData().get("fieldCount"));
        assertNotNull(result.getOutputData().get("parsed"));
    }

    @Test
    void parsedOutputMatchesSourceJson() {
        Map<String, Object> source = Map.of("cust_id", "C-100", "email", "test@test.com");
        Task task = taskWith(Map.of("sourceJson", source));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsed");
        assertEquals("C-100", parsed.get("cust_id"));
        assertEquals("test@test.com", parsed.get("email"));
    }

    @Test
    void fieldCountMatchesNumberOfKeys() {
        Task task = taskWith(Map.of(
                "sourceJson", Map.of("a", 1, "b", 2, "c", 3)));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("fieldCount"));
    }

    @Test
    void handlesEmptySourceJson() {
        Task task = taskWith(Map.of("sourceJson", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("fieldCount"));
    }

    @Test
    void handlesNullSourceJson() {
        Map<String, Object> input = new HashMap<>();
        input.put("sourceJson", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("fieldCount"));
        assertNotNull(result.getOutputData().get("parsed"));
    }

    @Test
    void handlesMissingSourceJsonKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("fieldCount"));
    }

    @Test
    void parsesSingleFieldSource() {
        Task task = taskWith(Map.of("sourceJson", Map.of("onlyField", "value")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("fieldCount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsed");
        assertEquals("value", parsed.get("onlyField"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
