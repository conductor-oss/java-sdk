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

class ExtractDataWorkerTest {

    private final ExtractDataWorker worker = new ExtractDataWorker();

    @TempDir
    Path tempDir;

    @Test
    void taskDefName() {
        assertEquals("el_extract_data", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsFromJsonString() {
        String json = "[{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@example.com\",\"amount\":\"150.50\"},"
                + "{\"id\":2,\"name\":\"Bob\",\"email\":\"bob@example.com\",\"amount\":\"200.00\"}]";
        Task task = taskWith(Map.of("jsonData", json));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(2, records.size());
        assertEquals(2, result.getOutputData().get("recordCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsFromCsvString() {
        String csv = "id,name,email,amount\n1,Alice,alice@example.com,150.50\n2,Bob,bob@example.com,200.00";
        Task task = taskWith(Map.of("csvData", csv));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(2, records.size());
        assertEquals(1, records.get(0).get("id"));
        assertEquals("Alice", records.get(0).get("name"));
        assertEquals(150.50, records.get(0).get("amount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsFromJsonFile() throws Exception {
        String json = "[{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@example.com\",\"amount\":\"150.50\"}]";
        File jsonFile = tempDir.resolve("data.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        Task task = taskWith(Map.of("source", jsonFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, records.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsFromCsvFile() throws Exception {
        String csv = "id,name,email,amount\n1,Alice,alice@example.com,150.50\n2,Bob,bob@example.com,200.00\n3,Charlie,charlie@example.com,0.00";
        File csvFile = tempDir.resolve("data.csv").toFile();
        Files.writeString(csvFile.toPath(), csv);

        Task task = taskWith(Map.of("source", csvFile.getAbsolutePath()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(3, records.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void recordsContainRequiredFields() {
        String json = "[{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@example.com\",\"amount\":\"150.50\"}]";
        Task task = taskWith(Map.of("jsonData", json));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        Map<String, Object> record = records.get(0);
        assertNotNull(record.get("id"));
        assertNotNull(record.get("name"));
        assertNotNull(record.get("email"));
        assertNotNull(record.get("amount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void recordCountMatchesRecordsList() {
        String json = "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"},{\"id\":3,\"name\":\"C\"}]";
        Task task = taskWith(Map.of("jsonData", json));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        int recordCount = (int) result.getOutputData().get("recordCount");
        assertEquals(records.size(), recordCount);
    }

    @Test
    void failsForMissingSourceFile() {
        Task task = taskWith(Map.of("source", "/nonexistent/path/data.json"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("not found"));
    }

    @Test
    void failsWhenNoInputProvided() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void failsForNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("source", null);
        input.put("jsonData", null);
        input.put("csvData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void csvParsesNumericValues() {
        String csv = "id,score,grade\n1,95.5,A\n2,88,B";
        Task task = taskWith(Map.of("csvData", csv));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(95.5, records.get(0).get("score"));
        assertEquals(88, records.get(1).get("score"));
        assertEquals("A", records.get(0).get("grade"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
