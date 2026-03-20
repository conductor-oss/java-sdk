package datamigration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateSourceWorkerTest {

    private final ValidateSourceWorker worker = new ValidateSourceWorker();

    @Test
    void taskDefName() {
        assertEquals("mi_validate_source", worker.getTaskDefName());
    }

    @Test
    void filtersOutInvalidRecords() {
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> valid = new LinkedHashMap<>();
        valid.put("name", "Alice"); valid.put("email", "alice@old.com");
        records.add(valid);

        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("name", null); invalid.put("email", "invalid");
        records.add(invalid);

        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void rejectsEmailWithoutAt() {
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", "Bob"); r.put("email", "noemail");
        records.add(r);

        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
