package datacompression.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeDataWorkerTest {

    private final AnalyzeDataWorker worker = new AnalyzeDataWorker();

    @Test
    void taskDefName() {
        assertEquals("cmp_analyze_data", worker.getTaskDefName());
    }

    @Test
    void analyzesRecords() {
        Task task = taskWith(Map.of("records", List.of(Map.of("id", 1, "data", "hello"), Map.of("id", 2, "data", "world"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("recordCount"));
        assertTrue((int) result.getOutputData().get("sizeBytes") > 0);
    }

    @Test
    void passesRecordsThrough() {
        Task task = taskWith(Map.of("records", List.of(Map.of("id", 1))));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void sizeGrowsWithMoreData() {
        Task small = taskWith(Map.of("records", List.of(Map.of("id", 1))));
        Task large = taskWith(Map.of("records", List.of(Map.of("id", 1, "data", "lots of extra data here to increase size"))));
        TaskResult smallResult = worker.execute(small);
        TaskResult largeResult = worker.execute(large);

        assertTrue((int) largeResult.getOutputData().get("sizeBytes") > (int) smallResult.getOutputData().get("sizeBytes"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
