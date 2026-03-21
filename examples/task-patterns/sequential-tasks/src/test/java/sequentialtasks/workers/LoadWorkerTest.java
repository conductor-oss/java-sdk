package sequentialtasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadWorkerTest {

    private final LoadWorker worker = new LoadWorker();

    @Test
    void taskDefName() {
        assertEquals("seq_load", worker.getTaskDefName());
    }

    @Test
    void loadsTransformedData() {
        List<Map<String, Object>> transformedData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85, "grade", "B", "normalizedScore", 0.85),
                Map.of("id", 2, "name", "Bob", "score", 92, "grade", "A", "normalizedScore", 0.92),
                Map.of("id", 3, "name", "Carol", "score", 78, "grade", "C", "normalizedScore", 0.78)
        );

        Task task = taskWith(Map.of("transformedData", transformedData));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("loaded"));
        assertEquals(3, result.getOutputData().get("totalRecords"));
    }

    @Test
    void returnsTotalRecordCount() {
        List<Map<String, Object>> transformedData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85, "grade", "B", "normalizedScore", 0.85)
        );

        Task task = taskWith(Map.of("transformedData", transformedData));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("totalRecords"));
    }

    @Test
    void returnsLoadedTrue() {
        List<Map<String, Object>> transformedData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85, "grade", "B", "normalizedScore", 0.85),
                Map.of("id", 2, "name", "Bob", "score", 92, "grade", "A", "normalizedScore", 0.92)
        );

        Task task = taskWith(Map.of("transformedData", transformedData));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("loaded"));
    }

    @Test
    void completesSuccessfully() {
        List<Map<String, Object>> transformedData = List.of(
                Map.of("id", 1, "name", "Test", "score", 50, "grade", "C", "normalizedScore", 0.5)
        );

        Task task = taskWith(Map.of("transformedData", transformedData));
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
