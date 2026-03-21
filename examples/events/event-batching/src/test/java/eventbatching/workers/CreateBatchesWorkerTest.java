package eventbatching.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateBatchesWorkerTest {

    private final CreateBatchesWorker worker = new CreateBatchesWorker();

    @Test
    void taskDefName() {
        assertEquals("eb_create_batches", worker.getTaskDefName());
    }

    @Test
    void createsTwoBatchesFromSixEvents() {
        Task task = taskWith(Map.of(
                "events", sixEvents(),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("batchCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void eachBatchHasThreeEvents() {
        Task task = taskWith(Map.of(
                "events", sixEvents(),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        List<List<Map<String, Object>>> batches =
                (List<List<Map<String, Object>>>) result.getOutputData().get("batches");
        assertEquals(2, batches.size());
        assertEquals(3, batches.get(0).size());
        assertEquals(3, batches.get(1).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void firstBatchContainsFirstThreeEvents() {
        Task task = taskWith(Map.of(
                "events", sixEvents(),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        List<List<Map<String, Object>>> batches =
                (List<List<Map<String, Object>>>) result.getOutputData().get("batches");
        assertEquals("e1", batches.get(0).get(0).get("id"));
        assertEquals("e2", batches.get(0).get(1).get("id"));
        assertEquals("e3", batches.get(0).get(2).get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void secondBatchContainsLastThreeEvents() {
        Task task = taskWith(Map.of(
                "events", sixEvents(),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        List<List<Map<String, Object>>> batches =
                (List<List<Map<String, Object>>>) result.getOutputData().get("batches");
        assertEquals("e4", batches.get(1).get(0).get("id"));
        assertEquals("e5", batches.get(1).get(1).get("id"));
        assertEquals("e6", batches.get(1).get(2).get("id"));
    }

    @Test
    void handlesUnevenBatches() {
        Task task = taskWith(Map.of(
                "events", List.of(
                        Map.of("id", "e1", "type", "click"),
                        Map.of("id", "e2", "type", "view"),
                        Map.of("id", "e3", "type", "click"),
                        Map.of("id", "e4", "type", "purchase"),
                        Map.of("id", "e5", "type", "view")),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("batchCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void unevenBatchLastBatchIsShorter() {
        Task task = taskWith(Map.of(
                "events", List.of(
                        Map.of("id", "e1", "type", "click"),
                        Map.of("id", "e2", "type", "view"),
                        Map.of("id", "e3", "type", "click"),
                        Map.of("id", "e4", "type", "purchase"),
                        Map.of("id", "e5", "type", "view")),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        List<List<Map<String, Object>>> batches =
                (List<List<Map<String, Object>>>) result.getOutputData().get("batches");
        assertEquals(3, batches.get(0).size());
        assertEquals(2, batches.get(1).size());
    }

    @Test
    void handlesEmptyEventList() {
        Task task = taskWith(Map.of(
                "events", List.of(),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchCount"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("batchSize", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchCount"));
    }

    @Test
    void defaultsBatchSizeToThree() {
        Task task = taskWith(Map.of(
                "events", sixEvents()));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("batchCount"));
    }

    private List<Map<String, Object>> sixEvents() {
        return List.of(
                Map.of("id", "e1", "type", "click"),
                Map.of("id", "e2", "type", "view"),
                Map.of("id", "e3", "type", "click"),
                Map.of("id", "e4", "type", "purchase"),
                Map.of("id", "e5", "type", "view"),
                Map.of("id", "e6", "type", "click"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
