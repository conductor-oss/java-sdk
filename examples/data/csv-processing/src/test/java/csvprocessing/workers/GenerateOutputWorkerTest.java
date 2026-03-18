package csvprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateOutputWorkerTest {

    private final GenerateOutputWorker worker = new GenerateOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("cv_generate_output", worker.getTaskDefName());
    }

    @Test
    void computesRecordCount() {
        Task task = taskWith(Map.of(
                "transformedRows", List.of(
                        Map.of("fullName", "Alice", "emailAddress", "alice@corp.com", "department", "ENGINEERING", "salary", 95000.0),
                        Map.of("fullName", "Bob", "emailAddress", "bob@corp.com", "department", "SALES", "salary", 82000.0)),
                "totalParsed", 4,
                "totalValid", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("recordCount"));
    }

    @Test
    void computesAverageSalaryInSummary() {
        Task task = taskWith(Map.of(
                "transformedRows", List.of(
                        Map.of("fullName", "Alice", "salary", 90000.0),
                        Map.of("fullName", "Bob", "salary", 110000.0)),
                "totalParsed", 3,
                "totalValid", 2));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("avg salary $100000.00"), "Expected avg salary $100000.00 in: " + summary);
    }

    @Test
    void summaryIncludesTotalParsedAndValid() {
        Task task = taskWith(Map.of(
                "transformedRows", List.of(
                        Map.of("fullName", "Alice", "salary", 95000.0)),
                "totalParsed", 4,
                "totalValid", 1));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("Processed 4 rows"), "Expected 'Processed 4 rows' in: " + summary);
        assertTrue(summary.contains("1 valid"), "Expected '1 valid' in: " + summary);
    }

    @Test
    void returnsRecordsInOutput() {
        List<Map<String, Object>> rows = List.of(
                Map.of("fullName", "Alice", "emailAddress", "alice@corp.com", "department", "ENGINEERING", "salary", 95000.0));
        Task task = taskWith(Map.of(
                "transformedRows", rows,
                "totalParsed", 1,
                "totalValid", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, records.size());
        assertEquals("Alice", records.get(0).get("fullName"));
    }

    @Test
    void handlesNullTransformedRows() {
        Map<String, Object> input = new HashMap<>();
        input.put("transformedRows", null);
        input.put("totalParsed", 0);
        input.put("totalValid", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesEmptyTransformedRows() {
        Task task = taskWith(Map.of(
                "transformedRows", List.of(),
                "totalParsed", 0,
                "totalValid", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("avg salary $0.00"), "Expected avg salary $0.00 in: " + summary);
    }

    @Test
    void handlesTotalParsedAsString() {
        Task task = taskWith(Map.of(
                "transformedRows", List.of(
                        Map.of("fullName", "Alice", "salary", 50000.0)),
                "totalParsed", "3",
                "totalValid", "1"));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("Processed 3 rows"), "Expected 'Processed 3 rows' in: " + summary);
        assertTrue(summary.contains("1 valid"), "Expected '1 valid' in: " + summary);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
