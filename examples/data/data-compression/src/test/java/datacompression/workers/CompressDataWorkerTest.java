package datacompression.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompressDataWorkerTest {

    private final CompressDataWorker worker = new CompressDataWorker();

    @Test
    void taskDefName() {
        assertEquals("cmp_compress_data", worker.getTaskDefName());
    }

    @Test
    void compressesWithGzip() {
        Task task = taskWith(Map.of("originalSize", 1000, "algorithm", "gzip"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(350, result.getOutputData().get("compressedSize"));
        assertNotNull(result.getOutputData().get("checksum"));
    }

    @Test
    void compressesWithLz4() {
        Task task = taskWith(Map.of("originalSize", 1000, "algorithm", "lz4"));
        TaskResult result = worker.execute(task);

        assertEquals(600, result.getOutputData().get("compressedSize"));
    }

    @Test
    void compressesWithZstd() {
        Task task = taskWith(Map.of("originalSize", 1000, "algorithm", "zstd"));
        TaskResult result = worker.execute(task);

        assertEquals(250, result.getOutputData().get("compressedSize"));
    }

    @Test
    void checksumStartsWithSha256() {
        Task task = taskWith(Map.of("originalSize", 100, "algorithm", "gzip"));
        TaskResult result = worker.execute(task);

        String checksum = (String) result.getOutputData().get("checksum");
        assertTrue(checksum.startsWith("sha256:"));
    }

    @Test
    void handlesZeroOriginalSize() {
        Task task = taskWith(Map.of("originalSize", 0, "algorithm", "gzip"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("compressedSize"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
