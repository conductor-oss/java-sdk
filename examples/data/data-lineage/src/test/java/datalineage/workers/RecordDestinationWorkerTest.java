package datalineage.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordDestinationWorkerTest {

    private final RecordDestinationWorker worker = new RecordDestinationWorker();

    @Test
    void taskDefName() {
        assertEquals("ln_record_destination", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void recordsDestination() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1)),
                "lineage", List.of(Map.of("step", "source")),
                "destName", "analytics_redshift"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> lineage = (List<Map<String, Object>>) result.getOutputData().get("lineage");
        assertEquals(2, lineage.size());
        assertEquals("destination", lineage.get(1).get("step"));
        assertEquals("analytics_redshift", lineage.get(1).get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void lineageContainsLoadOperation() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1)),
                "lineage", List.of(),
                "destName", "warehouse"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> lineage = (List<Map<String, Object>>) result.getOutputData().get("lineage");
        assertEquals("load", lineage.get(0).get("operation"));
    }

    @Test
    void passesRecordsThrough() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1, "name", "Alice")),
                "lineage", List.of(),
                "destName", "dest"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void handlesEmptyInputs() {
        Task task = taskWith(Map.of("records", List.of(), "lineage", List.of(), "destName", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("lineage", null);
        input.put("destName", "test");
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
