package datalineage.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegisterSourceWorkerTest {

    private final RegisterSourceWorker worker = new RegisterSourceWorker();

    @Test
    void taskDefName() {
        assertEquals("ln_register_source", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void registersSourceWithLineage() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2)),
                "sourceName", "crm_db"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("recordCount"));
        List<Map<String, Object>> lineage = (List<Map<String, Object>>) result.getOutputData().get("lineage");
        assertEquals(1, lineage.size());
        assertEquals("source", lineage.get(0).get("step"));
        assertEquals("crm_db", lineage.get(0).get("name"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "sourceName", "empty_source"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("sourceName", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void lineageContainsExtractOperation() {
        Task task = taskWith(Map.of("records", List.of(Map.of("id", 1)), "sourceName", "src"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> lineage = (List<Map<String, Object>>) result.getOutputData().get("lineage");
        assertEquals("extract", lineage.get(0).get("operation"));
    }

    @Test
    void passesRecordsThrough() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Alice"));
        Task task = taskWith(Map.of("records", records, "sourceName", "test"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("records"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
