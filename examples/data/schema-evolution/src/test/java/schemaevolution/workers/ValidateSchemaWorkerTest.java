package schemaevolution.workers;

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
        assertEquals("sh_validate_schema", worker.getTaskDefName());
    }

    @Test
    void passesWhenAllRecordsValid() {
        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "Alice", "phone_number", "555-0101", "age", 30, "status", "active"));
        Task task = taskWith(Map.of("transformedData", data));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("passed"));
        assertEquals("FULL", result.getOutputData().get("compatibilityLevel"));
    }

    @Test
    void failsWhenLegacyIdPresent() {
        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "phone_number", "555", "age", 30, "legacy_id", "L001"));
        Task task = taskWith(Map.of("transformedData", data));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("passed"));
        assertEquals("PARTIAL", result.getOutputData().get("compatibilityLevel"));
    }

    @Test
    void failsWhenAgeIsString() {
        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "phone_number", "555", "age", "30"));
        Task task = taskWith(Map.of("transformedData", data));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("passed"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of("transformedData", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("passed"));
        assertEquals(0, result.getOutputData().get("validatedRecords"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
