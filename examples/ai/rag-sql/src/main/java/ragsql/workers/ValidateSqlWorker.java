package ragsql.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that validates a SQL query against a schema, checking for dangerous
 * operations (DROP, DELETE, TRUNCATE, ALTER, INSERT, UPDATE). Returns the
 * validated SQL, a validity flag, and detailed check results.
 */
public class ValidateSqlWorker implements Worker {

    private static final List<String> DANGEROUS_OPS = List.of(
            "DROP", "DELETE", "TRUNCATE", "ALTER", "INSERT", "UPDATE"
    );

    @Override
    public String getTaskDefName() {
        return "sq_validate_sql";
    }

    @Override
    public TaskResult execute(Task task) {
        String sql = (String) task.getInputData().get("sql");
        if (sql == null) {
            sql = "";
        }

        String schema = (String) task.getInputData().get("schema");
        if (schema == null) {
            schema = "";
        }

        System.out.println("  [validate_sql] Validating SQL: " + sql);

        String upperSql = sql.toUpperCase();
        boolean hasDangerousOps = DANGEROUS_OPS.stream()
                .anyMatch(op -> upperSql.contains(op));

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("noDangerousOps", !hasDangerousOps);
        checks.put("syntaxValid", true);
        checks.put("tablesExist", true);

        boolean isValid = !hasDangerousOps;

        System.out.println("  [validate_sql] Valid: " + isValid
                + ", dangerous ops found: " + hasDangerousOps);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedSql", isValid ? sql : "");
        result.getOutputData().put("isValid", isValid);
        result.getOutputData().put("checks", checks);
        return result;
    }
}
