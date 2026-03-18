package databaseagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteQueryWorkerTest {

    private final ExecuteQueryWorker worker = new ExecuteQueryWorker();

    @BeforeEach
    void setUp() {
        ExecuteQueryWorker.resetDatabase();
    }

    @Test
    void taskDefName() {
        assertEquals("db_execute_query", worker.getTaskDefName());
    }

    @Test
    void selectAllReturnsFiveSeededRows() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    void returnsRowCountMatchingResults() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(results.size(), result.getOutputData().get("rowCount"));
    }

    @Test
    void returnsExecutionTime() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("executionTimeMs"));
    }

    @Test
    void resultRowsContainExpectedColumns() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        Map<String, Object> firstRow = results.get(0);
        assertTrue(firstRow.containsKey("department"));
        assertTrue(firstRow.containsKey("employee_count"));
        assertTrue(firstRow.containsKey("total_revenue"));
        assertTrue(firstRow.containsKey("avg_sale"));
    }

    @Test
    void firstRowIsEngineering() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals("Engineering", results.get(0).get("department"));
        assertEquals(45, results.get(0).get("employee_count"));
        assertEquals(2850000, results.get(0).get("total_revenue"));
    }

    @Test
    void selectWithWhereClause() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments WHERE department = 'Sales'", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(1, results.size());
        assertEquals("Sales", results.get(0).get("department"));
    }

    @Test
    void selectFromEmployeesTable() {
        Task task = taskWith(Map.of("query", "SELECT * FROM employees", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(5, results.size());
        assertEquals("Alice", results.get(0).get("name"));
    }

    @Test
    void selectWithLimit() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments LIMIT 2", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(2, results.size());
    }

    @Test
    void insertAddsRow() {
        Task task = taskWith(Map.of("query",
                "INSERT INTO departments (department, employee_count, total_revenue, avg_sale) VALUES ('HR', 15, 900000, 60000)",
                "isReadOnly", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        // Verify the row was added
        Task selectTask = taskWith(Map.of("query", "SELECT * FROM departments WHERE department = 'HR'", "isReadOnly", true));
        TaskResult selectResult = worker.execute(selectTask);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) selectResult.getOutputData().get("results");
        assertEquals(1, results.size());
        assertEquals("HR", results.get(0).get("department"));
    }

    @Test
    void insertBlockedInReadOnlyMode() {
        Task task = taskWith(Map.of("query",
                "INSERT INTO departments (department) VALUES ('Test')",
                "isReadOnly", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Should return error about read-only
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertTrue(results.get(0).containsKey("error"));
    }

    @Test
    void deleteRemovesRow() {
        Task task = taskWith(Map.of("query",
                "DELETE FROM departments WHERE department = 'Operations'",
                "isReadOnly", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        // Verify the row was removed
        Task selectTask = taskWith(Map.of("query", "SELECT * FROM departments", "isReadOnly", true));
        TaskResult selectResult = worker.execute(selectTask);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) selectResult.getOutputData().get("results");
        assertEquals(4, results.size());
    }

    @Test
    void selectLiteralWorks() {
        Task task = taskWith(Map.of("query", "SELECT 1", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).get("result"));
    }

    @Test
    void handlesNullQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", null);
        input.put("isReadOnly", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void handlesMissingIsReadOnly() {
        Task task = taskWith(Map.of("query", "SELECT * FROM departments"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("rowCount"));
    }

    @Test
    void handlesBlankQuery() {
        Task task = taskWith(Map.of("query", "   ", "isReadOnly", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    @Test
    void nonexistentTableReturnsError() {
        Task task = taskWith(Map.of("query", "SELECT * FROM nonexistent", "isReadOnly", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertTrue(results.get(0).containsKey("error"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
