package etlbasics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmLoadWorkerTest {

    private final ConfirmLoadWorker worker = new ConfirmLoadWorker();

    @TempDir
    Path tempDir;

    @Test
    void taskDefName() {
        assertEquals("el_confirm_load", worker.getTaskDefName());
    }

    @Test
    void confirmsSuccessfulLoad() throws Exception {
        // Create a real JSON file with 3 records
        File outputFile = tempDir.resolve("output.json").toFile();
        Files.writeString(outputFile.toPath(), "[{\"id\":1},{\"id\":2},{\"id\":3}]");

        Task task = taskWith(Map.of("loadedCount", 3, "destination", outputFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ETL_COMPLETE", result.getOutputData().get("status"));
        assertEquals(3, result.getOutputData().get("loadedCount"));
        assertTrue((Boolean) result.getOutputData().get("verified"));
    }

    @Test
    void outputStatusIsEtlComplete() {
        Task task = taskWith(Map.of("loadedCount", 5, "destination", "test-db"));
        TaskResult result = worker.execute(task);

        assertEquals("ETL_COMPLETE", result.getOutputData().get("status"));
    }

    @Test
    void passesLoadedCountThrough() {
        Task task = taskWith(Map.of("loadedCount", 10, "destination", "warehouse"));
        TaskResult result = worker.execute(task);

        assertEquals(10, result.getOutputData().get("loadedCount"));
    }

    @Test
    void handlesZeroLoadedCount() {
        Task task = taskWith(Map.of("loadedCount", 0, "destination", "empty-db"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ETL_COMPLETE", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    @Test
    void handlesNullLoadedCount() {
        Map<String, Object> input = new HashMap<>();
        input.put("loadedCount", null);
        input.put("destination", "test-db");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    @Test
    void handlesNullDestination() {
        Map<String, Object> input = new HashMap<>();
        input.put("loadedCount", 3);
        input.put("destination", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ETL_COMPLETE", result.getOutputData().get("status"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ETL_COMPLETE", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    @Test
    void reportsFileSizeWhenFileExists() throws Exception {
        File outputFile = tempDir.resolve("sized.json").toFile();
        String content = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
        Files.writeString(outputFile.toPath(), content);

        Task task = taskWith(Map.of("loadedCount", 2, "destination", outputFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        long fileSize = ((Number) result.getOutputData().get("fileSize")).longValue();
        assertTrue(fileSize > 0, "File size should be reported");
    }

    @Test
    void detectsMismatchedRecordCount() throws Exception {
        // File has 2 records but loadedCount says 5
        File outputFile = tempDir.resolve("mismatch.json").toFile();
        Files.writeString(outputFile.toPath(), "[{\"id\":1},{\"id\":2}]");

        Task task = taskWith(Map.of("loadedCount", 5, "destination", outputFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertFalse((Boolean) result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
