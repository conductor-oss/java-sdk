package ragsql.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that formats SQL query results into a natural-language answer.
 * Takes the original question, SQL, rows, and row count, and produces
 * a human-readable summary.
 */
public class FormatResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sq_format_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        String sql = (String) task.getInputData().get("sql");
        if (sql == null) {
            sql = "";
        }

        List<Map<String, Object>> rows = (List<Map<String, Object>>) task.getInputData().get("rows");
        if (rows == null) {
            rows = List.of();
        }

        Object rowCountObj = task.getInputData().get("rowCount");
        int rowCount = (rowCountObj instanceof Number) ? ((Number) rowCountObj).intValue() : rows.size();

        System.out.println("  [format_results] Formatting " + rowCount + " rows for question: \"" + question + "\"");

        StringBuilder sb = new StringBuilder();
        sb.append("Based on your question \"").append(question).append("\", ");
        sb.append("I found ").append(rowCount).append(" results. ");

        if (!rows.isEmpty()) {
            Map<String, Object> topRow = rows.get(0);
            sb.append("The top workflow is \"")
              .append(topRow.getOrDefault("workflow_name", "unknown"))
              .append("\" with ")
              .append(topRow.getOrDefault("execution_count", 0))
              .append(" executions and an average duration of ")
              .append(topRow.getOrDefault("avg_duration_sec", 0))
              .append(" seconds.");
        }

        String answer = sb.toString();

        System.out.println("  [format_results] Answer: " + answer);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
