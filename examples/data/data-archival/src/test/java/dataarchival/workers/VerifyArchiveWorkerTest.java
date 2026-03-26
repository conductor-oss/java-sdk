package dataarchival.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyArchiveWorkerTest {

    private final VerifyArchiveWorker worker = new VerifyArchiveWorker();

    @Test
    void taskDefName() {
        assertEquals("arc_verify_archive", worker.getTaskDefName());
    }

    @Test
    void verifiesValidArchive() {
        Task task = taskWith(Map.of("archivePath", "s3://test/archive.gz", "expectedCount", 5, "checksum", "sha256:abc"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void failsWithoutChecksum() {
        Map<String, Object> input = new HashMap<>();
        input.put("archivePath", "s3://test/archive.gz");
        input.put("expectedCount", 5);
        input.put("checksum", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsWithZeroCount() {
        Task task = taskWith(Map.of("archivePath", "s3://test", "expectedCount", 0, "checksum", "sha256:abc"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void preservesArchivePath() {
        Task task = taskWith(Map.of("archivePath", "s3://my-bucket/data.gz", "expectedCount", 1, "checksum", "sha256:x"));
        TaskResult result = worker.execute(task);

        assertEquals("s3://my-bucket/data.gz", result.getOutputData().get("archivePath"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
