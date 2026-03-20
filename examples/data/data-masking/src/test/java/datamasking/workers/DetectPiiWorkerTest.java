package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectPiiWorkerTest {

    private final DetectPiiWorker worker = new DetectPiiWorker();

    @Test
    void taskDefName() {
        assertEquals("mk_detect_pii", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void detectsAllPiiFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "ssn", "123-45-6789", "email", "a@b.com", "phone", "555-1234"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<String> fields = (List<String>) result.getOutputData().get("piiFields");
        assertEquals(3, fields.size());
        assertTrue(fields.contains("ssn"));
        assertTrue(fields.contains("email"));
        assertTrue(fields.contains("phone"));
    }

    @Test
    void detectsNoPiiInCleanRecords() {
        List<Map<String, Object>> records = List.of(Map.of("name", "Alice", "age", 30));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("piiFieldCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("piiFieldCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
