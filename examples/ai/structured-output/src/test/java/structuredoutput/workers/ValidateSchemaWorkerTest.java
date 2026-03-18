package structuredoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateSchemaWorkerTest {

    private final ValidateSchemaWorker worker = new ValidateSchemaWorker();

    @Test
    void taskDefName() {
        assertEquals("so_validate_schema", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validDataPassesValidation() {
        Map<String, Object> data = new HashMap<>(Map.of(
                "name", "Acme Corp",
                "industry", "Technology",
                "founded", 2015,
                "employees", 250
        ));
        Map<String, Object> schema = new HashMap<>(Map.of(
                "required", List.of("name", "industry", "founded", "employees"),
                "types", Map.of(
                        "name", "string",
                        "industry", "string",
                        "founded", "number",
                        "employees", "number"
                )
        ));

        Task task = taskWith(new HashMap<>(Map.of("jsonOutput", data, "schema", schema)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals(true, result.getOutputData().get("validated"));
        assertEquals(4, result.getOutputData().get("fieldCount"));

        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingFieldFailsValidation() {
        Map<String, Object> data = new HashMap<>(Map.of(
                "name", "Acme Corp",
                "industry", "Technology"
        ));
        Map<String, Object> schema = new HashMap<>(Map.of(
                "required", List.of("name", "industry", "founded", "employees"),
                "types", Map.of(
                        "name", "string",
                        "industry", "string",
                        "founded", "number",
                        "employees", "number"
                )
        ));

        Task task = taskWith(new HashMap<>(Map.of("jsonOutput", data, "schema", schema)));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));

        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("founded")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("employees")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void typeMismatchFailsValidation() {
        Map<String, Object> data = new HashMap<>(Map.of(
                "name", "Acme Corp",
                "industry", "Technology",
                "founded", "not-a-number",
                "employees", 250
        ));
        Map<String, Object> schema = new HashMap<>(Map.of(
                "required", List.of("name", "industry", "founded", "employees"),
                "types", Map.of(
                        "name", "string",
                        "industry", "string",
                        "founded", "number",
                        "employees", "number"
                )
        ));

        Task task = taskWith(new HashMap<>(Map.of("jsonOutput", data, "schema", schema)));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));

        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("founded"));
        assertTrue(errors.get(0).contains("Type mismatch"));
    }

    @Test
    void outputContainsDataAndFieldCount() {
        Map<String, Object> data = new HashMap<>(Map.of(
                "name", "Test",
                "industry", "Tech",
                "founded", 2020,
                "employees", 10,
                "extra", "field"
        ));
        Map<String, Object> schema = new HashMap<>(Map.of(
                "required", List.of("name"),
                "types", Map.of("name", "string")
        ));

        Task task = taskWith(new HashMap<>(Map.of("jsonOutput", data, "schema", schema)));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("fieldCount"));
        assertNotNull(result.getOutputData().get("data"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
