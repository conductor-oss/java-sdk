package datasampling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadDatasetWorkerTest {

    private final LoadDatasetWorker worker = new LoadDatasetWorker();

    @Test
    void taskDefName() {
        assertEquals("sm_load_dataset", worker.getTaskDefName());
    }

    @Test
    void loadsRecordsSuccessfully() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "value", 95),
                Map.of("name", "Bob", "value", 82));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(records, result.getOutputData().get("records"));
        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    void returnsCorrectCountForLargeDataset() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1),
                Map.of("name", "B", "value", 2),
                Map.of("name", "C", "value", 3),
                Map.of("name", "D", "value", 4),
                Map.of("name", "E", "value", 5));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("count"));
    }

    @Test
    void handlesEmptyRecordsList() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(List.of(), result.getOutputData().get("records"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(List.of(), result.getOutputData().get("records"));
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
    void preservesRecordContent() {
        Map<String, Object> record = Map.of("name", "TestUser", "value", 42, "extra", "data");
        Task task = taskWith(Map.of("records", List.of(record)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputRecords = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, outputRecords.size());
        assertEquals("TestUser", outputRecords.get(0).get("name"));
        assertEquals(42, outputRecords.get(0).get("value"));
        assertEquals("data", outputRecords.get(0).get("extra"));
    }

    @Test
    void singleRecordCountIsOne() {
        Task task = taskWith(Map.of("records", List.of(Map.of("name", "Solo", "value", 100))));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
