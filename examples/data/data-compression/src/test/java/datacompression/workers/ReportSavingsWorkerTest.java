package datacompression.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportSavingsWorkerTest {

    private final ReportSavingsWorker worker = new ReportSavingsWorker();

    @Test
    void taskDefName() {
        assertEquals("cmp_report_savings", worker.getTaskDefName());
    }

    @Test
    void calculatesRatio() {
        Task task = taskWith(Map.of("originalSize", 1000, "compressedSize", 500, "algorithm", "gzip", "verified", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("50.0%", result.getOutputData().get("compressionRatio"));
        assertEquals(500, result.getOutputData().get("bytesSaved"));
    }

    @Test
    void summaryContainsAlgorithm() {
        Task task = taskWith(Map.of("originalSize", 1000, "compressedSize", 250, "algorithm", "zstd", "verified", true));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("zstd"));
    }

    @Test
    void summaryContainsVerifiedStatus() {
        Task task = taskWith(Map.of("originalSize", 1000, "compressedSize", 500, "algorithm", "gzip", "verified", false));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("verified=false"));
    }

    @Test
    void handlesZeroCompressedSize() {
        Task task = taskWith(Map.of("originalSize", 1000, "compressedSize", 0, "algorithm", "gzip", "verified", true));
        TaskResult result = worker.execute(task);

        assertEquals("100.0%", result.getOutputData().get("compressionRatio"));
        assertEquals(1000, result.getOutputData().get("bytesSaved"));
    }

    @Test
    void handlesDefaultValues() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
