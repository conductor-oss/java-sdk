package ragsql.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateSqlWorkerTest {

    private final ValidateSqlWorker worker = new ValidateSqlWorker();

    @Test
    void taskDefName() {
        assertEquals("sq_validate_sql", worker.getTaskDefName());
    }

    @Test
    void validSelectQuery() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "SELECT workflow_name, COUNT(*) FROM workflow_executions GROUP BY workflow_name",
                "schema", "workflow_executions(id, workflow_name)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("isValid"));
        assertEquals(task.getInputData().get("sql"), result.getOutputData().get("validatedSql"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void checksContainAllFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "SELECT * FROM t",
                "schema", "t(id)"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> checks = (Map<String, Object>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertEquals(true, checks.get("noDangerousOps"));
        assertEquals(true, checks.get("syntaxValid"));
        assertEquals(true, checks.get("tablesExist"));
    }

    @Test
    void rejectsDropStatement() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "DROP TABLE workflow_executions",
                "schema", "workflow_executions(id)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("isValid"));
        assertEquals("", result.getOutputData().get("validatedSql"));
    }

    @Test
    void rejectsDeleteStatement() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "DELETE FROM workflow_executions WHERE id = 1",
                "schema", "workflow_executions(id)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
        assertEquals("", result.getOutputData().get("validatedSql"));
    }

    @Test
    void rejectsTruncateStatement() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "TRUNCATE TABLE workflow_executions",
                "schema", "workflow_executions(id)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void rejectsAlterStatement() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "ALTER TABLE workflow_executions ADD COLUMN x INT",
                "schema", "workflow_executions(id)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void rejectsInsertStatement() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "INSERT INTO workflow_executions VALUES (1, 'test')",
                "schema", "workflow_executions(id)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void rejectsUpdateStatement() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "UPDATE workflow_executions SET status = 'DELETED'",
                "schema", "workflow_executions(id)"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    @Test
    void handlesNullSql() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("isValid"));
    }

    @Test
    void caseInsensitiveDetection() {
        Task task = taskWith(new HashMap<>(Map.of(
                "sql", "drop table users"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("isValid"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
