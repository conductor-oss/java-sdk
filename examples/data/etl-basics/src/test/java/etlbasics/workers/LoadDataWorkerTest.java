package etlbasics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadDataWorkerTest {

    private final LoadDataWorker worker = new LoadDataWorker();

    @TempDir
    Path tempDir;

    @Test
    void taskDefName() {
        assertEquals("el_load_data", worker.getTaskDefName());
    }

    @Test
    void loadsRecordsToDestinationFile() {
        String destPath = tempDir.resolve("output.json").toString();
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob"),
                Map.of("id", 3, "name", "Charlie"));
        Task task = taskWith(Map.of("records", records, "destination", destPath));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("loadedCount"));
        // Verify file was actually written
        File outputFile = new File(destPath);
        assertTrue(outputFile.exists(), "Output file should exist");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");
    }

    @Test
    void writtenFileContainsValidJson() throws Exception {
        String destPath = tempDir.resolve("records.json").toString();
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob"));
        Task task = taskWith(Map.of("records", records, "destination", destPath));
        worker.execute(task);

        String content = Files.readString(Path.of(destPath));
        assertTrue(content.contains("Alice"), "Written file should contain record data");
        assertTrue(content.contains("Bob"), "Written file should contain record data");
    }

    @Test
    void loadedCountMatchesRecordsSize() {
        String destPath = tempDir.resolve("out.json").toString();
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "A"),
                Map.of("id", 2, "name", "B"));
        Task task = taskWith(Map.of("records", records, "destination", destPath));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("loadedCount"));
    }

    @Test
    void handlesEmptyRecords() {
        String destPath = tempDir.resolve("empty.json").toString();
        Task task = taskWith(Map.of("records", List.of(), "destination", destPath));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    @Test
    void handlesNullRecords() {
        String destPath = tempDir.resolve("null.json").toString();
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("destination", destPath);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    @Test
    void createsTempFileForNonPathDestination() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("id", 1)));
        input.put("destination", "analytics-warehouse");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String actualDest = (String) result.getOutputData().get("destination");
        // Should be an actual file path
        assertTrue(new File(actualDest).exists(), "Temp file should exist");
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    @Test
    void returnsDestinationInOutput() {
        String destPath = tempDir.resolve("dest.json").toString();
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1)),
                "destination", destPath));
        TaskResult result = worker.execute(task);

        assertEquals(destPath, result.getOutputData().get("destination"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
