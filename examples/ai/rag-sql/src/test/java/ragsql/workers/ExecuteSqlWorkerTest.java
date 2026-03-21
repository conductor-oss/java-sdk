package ragsql.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteSqlWorkerTest {

    private final ExecuteSqlWorker worker = new ExecuteSqlWorker();

    @Test
    void taskDefName() {
        assertEquals("sq_execute_sql", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsFiveRows() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "SELECT workflow_name, COUNT(*) FROM workflow_executions GROUP BY workflow_name"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertNotNull(rows);
        assertEquals(5, rows.size());
        assertEquals(5, result.getOutputData().get("rowCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rowsContainExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of("sql", "SELECT * FROM t")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        for (Map<String, Object> row : rows) {
            assertTrue(row.containsKey("workflow_name"));
            assertTrue(row.containsKey("execution_count"));
            assertTrue(row.containsKey("avg_duration_sec"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void firstRowIsOrderProcessing() {
        Task task = taskWith(new HashMap<>(Map.of("sql", "SELECT * FROM t")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        Map<String, Object> first = rows.get(0);
        assertEquals("order_processing", first.get("workflow_name"));
        assertEquals(1250, first.get("execution_count"));
        assertEquals(3.2, first.get("avg_duration_sec"));
    }

    @Test
    void handlesNullSql() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rows"));
        assertEquals(5, result.getOutputData().get("rowCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
