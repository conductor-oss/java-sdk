package sensordataprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateDataWorkerTest {

    private final ValidateDataWorker worker = new ValidateDataWorker();

    @Test
    void taskDefName() {
        assertEquals("sen_validate_data", worker.getTaskDefName());
    }

    @Test
    void validatesReadings() {
        Task task = taskWith(Map.of("batchId", "B-001", "readingCount", 1200, "readings", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1176, result.getOutputData().get("validCount"));
        assertEquals(24, result.getOutputData().get("invalidCount"));
    }

    @Test
    void validCountIs98PercentOfTotal() {
        Task task = taskWith(Map.of("readingCount", 1000, "readings", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(980, result.getOutputData().get("validCount"));
        assertEquals(20, result.getOutputData().get("invalidCount"));
    }

    @Test
    void passesReadingsThrough() {
        List<Map<String, Object>> readings = List.of(Map.of("sensorId", "S-001", "temperature", 72.5));
        Task task = taskWith(Map.of("readingCount", 100, "readings", readings));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("validatedData"));
    }

    @Test
    void outputContainsIssues() {
        Task task = taskWith(Map.of("readingCount", 500, "readings", List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.getOutputData().get("issues");
        assertEquals(2, issues.size());
    }

    @Test
    void handlesZeroReadingCount() {
        Task task = taskWith(Map.of("readingCount", 0, "readings", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("invalidCount"));
    }

    @Test
    void handlesStringReadingCount() {
        Task task = taskWith(Map.of("readingCount", "500", "readings", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(490, result.getOutputData().get("validCount"));
    }

    @Test
    void handlesMissingReadingCount() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
