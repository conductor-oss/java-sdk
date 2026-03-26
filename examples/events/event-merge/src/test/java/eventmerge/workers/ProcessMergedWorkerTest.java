package eventmerge.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessMergedWorkerTest {

    private final ProcessMergedWorker worker = new ProcessMergedWorker();

    @Test
    void taskDefName() {
        assertEquals("mg_process_merged", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "mergedEvents", List.of(Map.of("id", "a1")),
                "totalCount", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputStatusIsAllProcessed() {
        Task task = taskWith(Map.of(
                "mergedEvents", List.of(Map.of("id", "a1"), Map.of("id", "b1")),
                "totalCount", 2));
        TaskResult result = worker.execute(task);

        assertEquals("all_processed", result.getOutputData().get("status"));
    }

    @Test
    void outputCountMatchesTotalCount() {
        Task task = taskWith(Map.of(
                "mergedEvents", List.of(Map.of("id", "a1"), Map.of("id", "b1"), Map.of("id", "c1")),
                "totalCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void handlesFullMergedList() {
        List<Map<String, String>> merged = List.of(
                Map.of("id", "a1", "source", "api", "type", "click"),
                Map.of("id", "a2", "source", "api", "type", "view"),
                Map.of("id", "b1", "source", "mobile", "type", "tap"),
                Map.of("id", "b2", "source", "mobile", "type", "scroll"),
                Map.of("id", "b3", "source", "mobile", "type", "tap"),
                Map.of("id", "c1", "source", "iot", "type", "sensor_reading"));

        Task task = taskWith(Map.of("mergedEvents", merged, "totalCount", 6));
        TaskResult result = worker.execute(task);

        assertEquals("all_processed", result.getOutputData().get("status"));
        assertEquals(6, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullMergedEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("mergedEvents", null);
        input.put("totalCount", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_processed", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullTotalCountFallsBackToListSize() {
        Map<String, Object> input = new HashMap<>();
        input.put("mergedEvents", List.of(Map.of("id", "a1"), Map.of("id", "b1")));
        input.put("totalCount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_processed", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
