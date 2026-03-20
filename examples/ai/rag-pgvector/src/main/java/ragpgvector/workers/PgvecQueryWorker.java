package ragpgvector.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that builds a pgvector SQL query and returns default result rows.
 *
 * Takes embedding, table name, limit, and distanceMetric as input.
 * Constructs the SQL with the appropriate pgvector operator:
 *   cosine => {@code <=>}
 *   l2     => {@code <->}
 *   ip     => {@code <#>}
 *
 * In production this would execute the SQL against PostgreSQL with pgvector.
 */
public class PgvecQueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pgvec_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String table = (String) task.getInputData().get("table");
        if (table == null || table.isBlank()) {
            table = "documents";
        }

        String metric = (String) task.getInputData().get("distanceMetric");
        if (metric == null || metric.isBlank()) {
            metric = "cosine";
        }

        Object limitObj = task.getInputData().get("limit");
        int limit = 3;
        if (limitObj instanceof Number) {
            limit = ((Number) limitObj).intValue();
        }

        String operator = switch (metric) {
            case "l2" -> "<->";
            case "ip" -> "<#>";
            default -> "<=>";
        };

        String sqlQuery = "SELECT id, content, source, 1 - (embedding " + operator
                + " $1::vector) AS similarity FROM " + table
                + " ORDER BY embedding " + operator + " $1::vector LIMIT " + limit + ";";

        System.out.println("  [pgvector] Table: \"" + table + "\", metric: " + metric + ", operator: " + operator);
        System.out.println("  [pgvector] SQL: " + sqlQuery.substring(0, Math.min(80, sqlQuery.length())) + "...");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new HashMap<>(Map.of(
                "id", 1,
                "content", "pgvector adds vector similarity search to PostgreSQL with indexing support.",
                "source", "pg_docs.md",
                "similarity", 0.94
        )));
        rows.add(new HashMap<>(Map.of(
                "id", 2,
                "content", "Create indexes with ivfflat or hnsw for approximate nearest neighbor search.",
                "source", "indexing.md",
                "similarity", 0.89
        )));
        rows.add(new HashMap<>(Map.of(
                "id", 3,
                "content", "Supports L2 distance, inner product, and cosine distance operators.",
                "source", "operators.md",
                "similarity", 0.86
        )));

        System.out.println("  [pgvector] " + rows.size() + " rows returned (best similarity: 0.94)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rows", rows);
        result.getOutputData().put("sqlQuery", sqlQuery);
        return result;
    }
}
