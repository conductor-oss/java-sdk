package creatingworkers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SafeProcessWorkerTest {

    private final SafeProcessWorker worker = new SafeProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("safe_process", worker.getTaskDefName());
    }

    @Test
    void processesRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "value", "rec-1", "score", 85),
                Map.of("id", 2, "value", "rec-2", "score", 92)
        );
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue((Boolean) results.get(0).get("processed"));
    }

    @Test
    void outputContainsCount() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "value", "rec-1"),
                Map.of("id", 2, "value", "rec-2"),
                Map.of("id", 3, "value", "rec-3")
        );
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("processedCount"));
        assertNotNull(result.getOutputData().get("passCount"));
        assertNotNull(result.getOutputData().get("failCount"));
    }

    @Test
    void outputContainsSummary() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "value", "rec-1")
        );
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("Processed"));
        assertTrue(summary.contains("1"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedCount"));
        assertEquals("No records provided", result.getOutputData().get("summary"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
