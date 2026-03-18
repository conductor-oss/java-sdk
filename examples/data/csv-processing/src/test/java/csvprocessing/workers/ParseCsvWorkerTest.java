package csvprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseCsvWorkerTest {

    private final ParseCsvWorker worker = new ParseCsvWorker();

    @Test
    void taskDefName() {
        assertEquals("cv_parse_csv", worker.getTaskDefName());
    }

    @Test
    void parsesStandardCsv() {
        Task task = taskWith(Map.of(
                "csvData", "name,email,dept,salary\nAlice,alice@corp.com,engineering,95000\nBob,bob@corp.com,sales,82000",
                "delimiter", ",",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals(2, rows.size());
        assertEquals(2, result.getOutputData().get("rowCount"));
    }

    @Test
    void extractsHeadersCorrectly() {
        Task task = taskWith(Map.of(
                "csvData", "name,email,dept,salary\nAlice,alice@corp.com,engineering,95000",
                "delimiter", ",",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) result.getOutputData().get("headers");
        assertEquals(List.of("name", "email", "dept", "salary"), headers);
    }

    @Test
    void mapsValuesToHeaders() {
        Task task = taskWith(Map.of(
                "csvData", "name,email\nAlice,alice@corp.com",
                "delimiter", ",",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals("Alice", rows.get(0).get("name"));
        assertEquals("alice@corp.com", rows.get(0).get("email"));
    }

    @Test
    void handlesCustomDelimiter() {
        Task task = taskWith(Map.of(
                "csvData", "name;email;dept\nAlice;alice@corp.com;engineering",
                "delimiter", ";",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).get("name"));
        assertEquals("engineering", rows.get(0).get("dept"));
    }

    @Test
    void handlesEmptyCsvData() {
        Task task = taskWith(Map.of(
                "csvData", "",
                "delimiter", ",",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals(0, rows.size());
        assertEquals(0, result.getOutputData().get("rowCount"));
    }

    @Test
    void handlesNullCsvData() {
        Map<String, Object> input = new HashMap<>();
        input.put("csvData", null);
        input.put("delimiter", ",");
        input.put("hasHeader", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("rowCount"));
    }

    @Test
    void defaultsToCommaDelimiter() {
        Map<String, Object> input = new HashMap<>();
        input.put("csvData", "name,email\nAlice,alice@corp.com");
        input.put("delimiter", null);
        input.put("hasHeader", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).get("name"));
    }

    @Test
    void skipsBlankLines() {
        Task task = taskWith(Map.of(
                "csvData", "name,email\nAlice,alice@corp.com\n\n  \nBob,bob@corp.com",
                "delimiter", ",",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals(2, rows.size());
    }

    @Test
    void handlesRowWithFewerColumnsThanHeaders() {
        Task task = taskWith(Map.of(
                "csvData", "name,email,dept,salary\nAlice,alice@corp.com",
                "delimiter", ",",
                "hasHeader", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) result.getOutputData().get("rows");
        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).get("name"));
        assertEquals("", rows.get(0).get("dept"));
        assertEquals("", rows.get(0).get("salary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
