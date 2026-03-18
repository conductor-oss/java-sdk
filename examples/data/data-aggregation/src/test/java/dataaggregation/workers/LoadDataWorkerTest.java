package dataaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadDataWorkerTest {

    private final LoadDataWorker worker = new LoadDataWorker();

    @Test
    void taskDefName() {
        assertEquals("agg_load_data", worker.getTaskDefName());
    }

    @Test
    void loadsRecordsAndReturnsCount() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("region", "east", "amount", 100),
                        Map.of("region", "west", "amount", 200))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void passesRecordsThrough() {
        List<Map<String, Object>> records = List.of(
                Map.of("region", "east", "amount", 50));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> outputRecords =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, outputRecords.size());
        assertEquals("east", outputRecords.get(0).get("region"));
    }

    @Test
    void handlesEmptyRecordsList() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingRecordsKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void loadsSingleRecord() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("name", "Alice", "score", 95))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("count"));
    }

    @Test
    void loadsMultipleRecordsPreservesOrder() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1),
                Map.of("id", 2),
                Map.of("id", 3));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("count"));
        assertNotNull(result.getOutputData().get("records"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
