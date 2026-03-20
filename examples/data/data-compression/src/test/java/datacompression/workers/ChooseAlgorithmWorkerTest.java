package datacompression.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChooseAlgorithmWorkerTest {

    private final ChooseAlgorithmWorker worker = new ChooseAlgorithmWorker();

    @Test
    void taskDefName() {
        assertEquals("cmp_choose_algorithm", worker.getTaskDefName());
    }

    @Test
    void choosesLz4ForSmallData() {
        Task task = taskWith(Map.of("originalSizeBytes", 500));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("lz4", result.getOutputData().get("algorithm"));
        assertEquals(0.6, result.getOutputData().get("estimatedRatio"));
    }

    @Test
    void choosesGzipForMediumData() {
        Task task = taskWith(Map.of("originalSizeBytes", 5000));
        TaskResult result = worker.execute(task);

        assertEquals("gzip", result.getOutputData().get("algorithm"));
        assertEquals(0.35, result.getOutputData().get("estimatedRatio"));
    }

    @Test
    void choosesZstdForLargeData() {
        Task task = taskWith(Map.of("originalSizeBytes", 200000));
        TaskResult result = worker.execute(task);

        assertEquals("zstd", result.getOutputData().get("algorithm"));
        assertEquals(0.25, result.getOutputData().get("estimatedRatio"));
    }

    @Test
    void handlesZeroSize() {
        Task task = taskWith(Map.of("originalSizeBytes", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("lz4", result.getOutputData().get("algorithm"));
    }

    @Test
    void handlesMissingSize() {
        Task task = taskWith(Map.of());
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
