package datamigration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractSourceWorkerTest {

    private final ExtractSourceWorker worker = new ExtractSourceWorker();

    @Test
    void taskDefName() {
        assertEquals("mi_extract_source", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsFiveRecords() {
        Task task = taskWith(Map.of("sourceConfig", Map.of("database", "legacy_hr_db")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("recordCount"));
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(5, records.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void includesInvalidRecord() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        // Record 4 has null name
        boolean hasNullName = records.stream().anyMatch(r -> r.get("name") == null);
        assertTrue(hasNullName);
    }

    @Test
    void returnsSchema() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("schema"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
