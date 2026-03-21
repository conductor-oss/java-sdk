package dataarchival.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransferToColdWorkerTest {

    private final TransferToColdWorker worker = new TransferToColdWorker();

    @Test
    void taskDefName() {
        assertEquals("arc_transfer_to_cold", worker.getTaskDefName());
    }

    @Test
    void transfersToArchive() {
        Map<String, Object> snapshot = Map.of("recordCount", 3, "sizeBytes", 500);
        Task task = taskWith(Map.of("snapshot", snapshot, "coldStoragePath", "s3://my-bucket/archives"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("transferredCount"));
        String path = (String) result.getOutputData().get("archivePath");
        assertTrue(path.startsWith("s3://my-bucket/archives/archive_"));
    }

    @Test
    void generatesChecksum() {
        Map<String, Object> snapshot = Map.of("recordCount", 1, "sizeBytes", 100);
        Task task = taskWith(Map.of("snapshot", snapshot, "coldStoragePath", "s3://test"));
        TaskResult result = worker.execute(task);

        String checksum = (String) result.getOutputData().get("checksum");
        assertTrue(checksum.startsWith("sha256:"));
    }

    @Test
    void handlesNullSnapshot() {
        Map<String, Object> input = new HashMap<>();
        input.put("snapshot", null);
        input.put("coldStoragePath", "s3://test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("transferredCount"));
    }

    @Test
    void defaultsColdStoragePath() {
        Map<String, Object> snapshot = Map.of("recordCount", 1, "sizeBytes", 50);
        Task task = taskWith(Map.of("snapshot", snapshot));
        TaskResult result = worker.execute(task);

        String path = (String) result.getOutputData().get("archivePath");
        assertTrue(path.startsWith("s3://archive/"));
    }

    @Test
    void includesSizeBytes() {
        Map<String, Object> snapshot = Map.of("recordCount", 2, "sizeBytes", 999);
        Task task = taskWith(Map.of("snapshot", snapshot, "coldStoragePath", "s3://test"));
        TaskResult result = worker.execute(task);

        assertEquals(999, result.getOutputData().get("sizeBytes"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
