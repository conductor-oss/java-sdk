package databaseagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseQuestionWorkerTest {

    private final ParseQuestionWorker worker = new ParseQuestionWorker();

    @Test
    void taskDefName() {
        assertEquals("db_parse_question", worker.getTaskDefName());
    }

    @Test
    void returnsIntent() {
        Task task = taskWith(Map.of(
                "question", "What are the top 5 departments by revenue?",
                "databaseSchema", Map.of("database", "company_analytics", "tables", List.of())));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("aggregate_query", result.getOutputData().get("intent"));
    }

    @Test
    void returnsEntitiesWithExpectedKeys() {
        Task task = taskWith(Map.of(
                "question", "Show revenue by department",
                "databaseSchema", Map.of("database", "test_db", "tables", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> entities = (Map<String, Object>) result.getOutputData().get("entities");
        assertNotNull(entities);
        assertEquals("total_revenue", entities.get("metric"));
        assertEquals("department", entities.get("groupBy"));
        assertEquals("revenue_desc", entities.get("orderBy"));
        assertEquals(5, entities.get("limit"));
    }

    @Test
    void returnsRelevantTables() {
        Task task = taskWith(Map.of(
                "question", "List all employees",
                "databaseSchema", Map.of("database", "hr_db", "tables", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> tables = (List<String>) result.getOutputData().get("relevantTables");
        assertNotNull(tables);
        assertEquals(3, tables.size());
        assertTrue(tables.contains("employees"));
        assertTrue(tables.contains("sales"));
        assertTrue(tables.contains("departments"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("databaseSchema", Map.of("database", "test", "tables", List.of()));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("intent"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(Map.of(
                "databaseSchema", Map.of("database", "test", "tables", List.of())));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("entities"));
    }

    @Test
    void handlesNullDatabaseSchema() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "Some question");
        input.put("databaseSchema", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("relevantTables"));
    }

    @Test
    void handlesMissingDatabaseSchema() {
        Task task = taskWith(Map.of("question", "Some question"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("intent"));
    }

    @Test
    void handlesBlankQuestion() {
        Task task = taskWith(Map.of(
                "question", "   ",
                "databaseSchema", Map.of("database", "test", "tables", List.of())));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("entities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
