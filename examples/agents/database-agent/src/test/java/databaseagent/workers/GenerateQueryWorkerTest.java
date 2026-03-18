package databaseagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateQueryWorkerTest {

    private final GenerateQueryWorker worker = new GenerateQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("db_generate_query", worker.getTaskDefName());
    }

    @Test
    void returnsMultiLineSqlQuery() {
        Task task = taskWith(Map.of(
                "intent", "aggregate_query",
                "entities", Map.of("metric", "total_revenue"),
                "tables", List.of("employees", "sales", "departments")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String query = (String) result.getOutputData().get("query");
        assertNotNull(query);
        assertTrue(query.contains("SELECT"));
        assertTrue(query.contains("JOIN"));
        assertTrue(query.contains("GROUP BY"));
        assertTrue(query.contains("ORDER BY"));
        assertTrue(query.contains("LIMIT"));
    }

    @Test
    void returnsQueryTypeSelect() {
        Task task = taskWith(Map.of("intent", "aggregate_query"));
        TaskResult result = worker.execute(task);

        assertEquals("SELECT", result.getOutputData().get("queryType"));
    }

    @Test
    void returnsTablesUsed() {
        Task task = taskWith(Map.of("intent", "aggregate_query"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> tablesUsed = (List<String>) result.getOutputData().get("tablesUsed");
        assertNotNull(tablesUsed);
        assertEquals(3, tablesUsed.size());
        assertTrue(tablesUsed.contains("departments"));
        assertTrue(tablesUsed.contains("employees"));
        assertTrue(tablesUsed.contains("sales"));
    }

    @Test
    void returnsComplexityMedium() {
        Task task = taskWith(Map.of("intent", "aggregate_query"));
        TaskResult result = worker.execute(task);

        assertEquals("medium", result.getOutputData().get("complexity"));
    }

    @Test
    void handlesNullIntent() {
        Map<String, Object> input = new HashMap<>();
        input.put("intent", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("query"));
    }

    @Test
    void handlesMissingIntent() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("query"));
    }

    @Test
    void handlesBlankIntent() {
        Task task = taskWith(Map.of("intent", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("queryType"));
    }

    @Test
    void queryContainsAggregationFunctions() {
        Task task = taskWith(Map.of("intent", "aggregate_query"));
        TaskResult result = worker.execute(task);

        String query = (String) result.getOutputData().get("query");
        assertTrue(query.contains("COUNT"));
        assertTrue(query.contains("SUM"));
        assertTrue(query.contains("AVG"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
