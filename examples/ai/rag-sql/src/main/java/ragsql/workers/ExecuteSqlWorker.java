package ragsql.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that executes a SQL query and returns default result rows.
 * Returns 5 rows with workflow_name, execution_count, and avg_duration_sec.
 * In production this would execute against a real database.
 */
public class ExecuteSqlWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sq_execute_sql";
    }

    @Override
    public TaskResult execute(Task task) {
        String sql = (String) task.getInputData().get("sql");
        if (sql == null) {
            sql = "";
        }

        System.out.println("  [execute_sql] Executing SQL: " + sql);

        List<Map<String, Object>> rows = List.of(
                row("order_processing", 1250, 3.2),
                row("payment_flow", 980, 2.8),
                row("user_onboarding", 750, 5.1),
                row("data_pipeline", 620, 12.4),
                row("notification_send", 430, 1.5)
        );

        System.out.println("  [execute_sql] Returned " + rows.size() + " rows");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rows", rows);
        result.getOutputData().put("rowCount", rows.size());
        return result;
    }

    private Map<String, Object> row(String workflowName, int executionCount, double avgDurationSec) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("workflow_name", workflowName);
        r.put("execution_count", executionCount);
        r.put("avg_duration_sec", avgDurationSec);
        return r;
    }
}
