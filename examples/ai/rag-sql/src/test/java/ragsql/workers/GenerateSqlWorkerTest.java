package ragsql.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateSqlWorkerTest {

    private final GenerateSqlWorker worker = new GenerateSqlWorker();

    @Test
    void taskDefName() {
        assertEquals("sq_generate_sql", worker.getTaskDefName());
    }

    @Test
    void generatesSqlFromEntities() {
        Map<String, String> entities = new HashMap<>(Map.of(
                "table", "workflow_executions",
                "metric", "execution_count",
                "filter", "status = 'COMPLETED'",
                "groupBy", "workflow_name",
                "timeRange", "last 7 days"
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "intent", "aggregate_query",
                "entities", entities,
                "schema", "workflow_executions(id, workflow_name, status)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String sql = (String) result.getOutputData().get("sql");
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("workflow_executions"));
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("workflow_name"));
    }

    @Test
    void returnsConfidenceScore() {
        Map<String, String> entities = new HashMap<>(Map.of(
                "table", "workflow_executions",
                "metric", "execution_count"
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "intent", "aggregate_query",
                "entities", entities
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.92, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesNullIntent() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sql"));
    }

    @Test
    void handlesNullEntities() {
        Task task = taskWith(new HashMap<>(Map.of("intent", "aggregate_query")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String sql = (String) result.getOutputData().get("sql");
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
    }

    @Test
    void sqlContainsCorrectStructure() {
        Map<String, String> entities = new HashMap<>(Map.of(
                "table", "workflow_executions",
                "metric", "execution_count",
                "filter", "status = 'COMPLETED'",
                "groupBy", "workflow_name"
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "intent", "aggregate_query",
                "entities", entities
        )));
        TaskResult result = worker.execute(task);

        String sql = (String) result.getOutputData().get("sql");
        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("AVG(duration_sec)"));
        assertTrue(sql.contains("FROM workflow_executions"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("LIMIT 10"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
