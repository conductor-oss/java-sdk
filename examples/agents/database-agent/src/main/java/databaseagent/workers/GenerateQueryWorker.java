package databaseagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Generates a SQL query from the parsed intent and entities.
 * Produces a multi-line SELECT statement with JOIN, GROUP BY, ORDER BY, and LIMIT clauses.
 */
public class GenerateQueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "db_generate_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String intent = (String) task.getInputData().get("intent");
        if (intent == null || intent.isBlank()) {
            intent = "unknown";
        }

        @SuppressWarnings("unchecked")
        List<String> tables = (List<String>) task.getInputData().get("tables");

        System.out.println("  [db_generate_query] Generating SQL for intent: " + intent);

        String query = "SELECT d.name AS department,\n"
                + "       COUNT(e.id) AS employee_count,\n"
                + "       SUM(s.amount) AS total_revenue,\n"
                + "       AVG(s.amount) AS avg_sale\n"
                + "FROM departments d\n"
                + "JOIN employees e ON e.department_id = d.id\n"
                + "JOIN sales s ON s.employee_id = e.id\n"
                + "GROUP BY d.name\n"
                + "ORDER BY total_revenue DESC\n"
                + "LIMIT 5";

        List<String> tablesUsed = List.of("departments", "employees", "sales");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("query", query);
        result.getOutputData().put("queryType", "SELECT");
        result.getOutputData().put("tablesUsed", tablesUsed);
        result.getOutputData().put("complexity", "medium");
        return result;
    }
}
