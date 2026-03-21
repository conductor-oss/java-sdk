package externalpayload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("ep_generate", worker.getTaskDefName());
    }

    @Test
    void generatesSummaryWithCorrectRecordCount() {
        Task task = taskWith(Map.of("dataSize", 10000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals(10000, summary.get("recordCount"));
    }

    @Test
    void summaryContainsAvgSizeAndTotalBytes() {
        Task task = taskWith(Map.of("dataSize", 500));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");

        assertEquals(1024, summary.get("avgSize"));
        assertEquals(500L * 1024, ((Number) summary.get("totalBytes")).longValue());
    }

    @Test
    void returnsStorageRef() {
        Task task = taskWith(Map.of("dataSize", 100));
        TaskResult result = worker.execute(task);

        assertEquals("s3://payloads/data.json", result.getOutputData().get("storageRef"));
    }

    @Test
    void defaultsDataSizeWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals(1000, summary.get("recordCount"));
    }

    @Test
    void defaultsDataSizeWhenZero() {
        Task task = taskWith(Map.of("dataSize", 0));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals(1000, summary.get("recordCount"));
    }

    @Test
    void defaultsDataSizeWhenNegative() {
        Task task = taskWith(Map.of("dataSize", -5));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals(1000, summary.get("recordCount"));
    }

    @Test
    void calculatesCorrectTotalBytes() {
        Task task = taskWith(Map.of("dataSize", 2048));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");

        long expectedBytes = 2048L * 1024;
        assertEquals(expectedBytes, ((Number) summary.get("totalBytes")).longValue());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
