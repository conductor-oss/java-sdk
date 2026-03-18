package databaseagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes a validated SQL query against an in-memory data store.
 *
 * <p>Maintains a static {@link ConcurrentHashMap} as an in-memory database of
 * tables, each table being a {@code List<Map<String, Object>>} of rows. The
 * store is seeded with sample department data on first use.
 *
 * <p>Supports the following SQL operations via simple regex parsing:
 * <ul>
 *   <li><b>SELECT</b> -- returns matching rows (with optional WHERE clause on
 *       a single column)</li>
 *   <li><b>INSERT INTO</b> -- adds a new row to the target table</li>
 *   <li><b>UPDATE</b> -- modifies matching rows</li>
 *   <li><b>DELETE FROM</b> -- removes matching rows</li>
 * </ul>
 *
 * <p>Read-only mode (default) rejects INSERT/UPDATE/DELETE operations.
 */
public class ExecuteQueryWorker implements Worker {

    /** In-memory database: table name to list of row maps. */
    static final ConcurrentHashMap<String, List<Map<String, Object>>> DATABASE = new ConcurrentHashMap<>();

    private static volatile boolean seeded = false;

    @Override
    public String getTaskDefName() {
        return "db_execute_query";
    }

    @Override
    public TaskResult execute(Task task) {
        ensureSeeded();

        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "SELECT 1";
        }

        Object isReadOnlyObj = task.getInputData().get("isReadOnly");
        boolean isReadOnly = isReadOnlyObj == null || Boolean.TRUE.equals(isReadOnlyObj);

        System.out.println("  [db_execute_query] Executing query (readOnly=" + isReadOnly + ")");

        long start = System.currentTimeMillis();
        String upperQuery = query.trim().toUpperCase();

        List<Map<String, Object>> results;
        int rowCount;

        try {
            if (upperQuery.startsWith("SELECT")) {
                results = executeSelect(query);
                rowCount = results.size();
            } else if (upperQuery.startsWith("INSERT")) {
                if (isReadOnly) {
                    return errorResult(task, "Write operations not allowed in read-only mode");
                }
                rowCount = executeInsert(query);
                results = List.of(Map.of("affectedRows", rowCount));
            } else if (upperQuery.startsWith("UPDATE")) {
                if (isReadOnly) {
                    return errorResult(task, "Write operations not allowed in read-only mode");
                }
                rowCount = executeUpdate(query);
                results = List.of(Map.of("affectedRows", rowCount));
            } else if (upperQuery.startsWith("DELETE")) {
                if (isReadOnly) {
                    return errorResult(task, "Write operations not allowed in read-only mode");
                }
                rowCount = executeDelete(query);
                results = List.of(Map.of("affectedRows", rowCount));
            } else {
                results = List.of(Map.of("message", "Unsupported query type"));
                rowCount = 0;
            }
        } catch (Exception e) {
            return errorResult(task, "Query execution error: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("rowCount", rowCount);
        result.getOutputData().put("executionTimeMs", elapsed);
        return result;
    }

    // ---- SQL operations --------------------------------------------------------

    private List<Map<String, Object>> executeSelect(String query) {
        String upper = query.trim().toUpperCase();

        // Handle SELECT 1 / SELECT literal
        if (upper.matches("SELECT\\s+\\d+.*")) {
            Matcher m = Pattern.compile("(?i)SELECT\\s+(\\d+)").matcher(query.trim());
            if (m.find()) {
                return List.of(Map.of("result", Integer.parseInt(m.group(1))));
            }
        }

        // Extract table name: SELECT ... FROM <table> ...
        Matcher fromMatcher = Pattern.compile("(?i)FROM\\s+(\\w+)").matcher(query);
        if (!fromMatcher.find()) {
            return List.of(Map.of("error", "Could not parse table name from query"));
        }
        String tableName = fromMatcher.group(1).toLowerCase();

        List<Map<String, Object>> table = DATABASE.get(tableName);
        if (table == null) {
            return List.of(Map.of("error", "Table not found: " + tableName));
        }

        // Check for WHERE clause
        Matcher whereMatcher = Pattern.compile("(?i)WHERE\\s+(\\w+)\\s*=\\s*'?([^'\\s;]+)'?").matcher(query);
        if (whereMatcher.find()) {
            String col = whereMatcher.group(1).toLowerCase();
            String val = whereMatcher.group(2);
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> row : table) {
                Object cellVal = row.get(col);
                if (cellVal != null && cellVal.toString().equalsIgnoreCase(val)) {
                    filtered.add(new LinkedHashMap<>(row));
                }
            }
            return filtered;
        }

        // Check for ORDER BY (simple single-column sort)
        Matcher orderMatcher = Pattern.compile("(?i)ORDER\\s+BY\\s+(\\w+)(?:\\s+(ASC|DESC))?").matcher(query);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> row : table) {
            resultList.add(new LinkedHashMap<>(row));
        }

        if (orderMatcher.find()) {
            String orderCol = orderMatcher.group(1).toLowerCase();
            boolean desc = orderMatcher.group(2) != null && orderMatcher.group(2).equalsIgnoreCase("DESC");
            resultList.sort((a, b) -> {
                Object va = a.get(orderCol);
                Object vb = b.get(orderCol);
                if (va instanceof Comparable && vb instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int cmp = ((Comparable<Object>) va).compareTo(vb);
                    return desc ? -cmp : cmp;
                }
                return 0;
            });
        }

        // Check for LIMIT
        Matcher limitMatcher = Pattern.compile("(?i)LIMIT\\s+(\\d+)").matcher(query);
        if (limitMatcher.find()) {
            int limit = Integer.parseInt(limitMatcher.group(1));
            if (limit < resultList.size()) {
                resultList = resultList.subList(0, limit);
            }
        }

        return resultList;
    }

    private int executeInsert(String query) {
        // INSERT INTO <table> (col1, col2, ...) VALUES (val1, val2, ...)
        Matcher m = Pattern.compile(
                "(?i)INSERT\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)"
        ).matcher(query);
        if (!m.find()) {
            throw new IllegalArgumentException("Cannot parse INSERT statement");
        }

        String tableName = m.group(1).toLowerCase();
        String[] cols = m.group(2).split(",");
        String[] vals = m.group(3).split(",");

        if (cols.length != vals.length) {
            throw new IllegalArgumentException("Column count does not match value count");
        }

        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < cols.length; i++) {
            String col = cols[i].trim().toLowerCase();
            String val = vals[i].trim().replaceAll("^'|'$", "");
            row.put(col, parseValue(val));
        }

        DATABASE.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
        return 1;
    }

    private int executeUpdate(String query) {
        // UPDATE <table> SET col1 = val1, ... WHERE col = val
        Matcher m = Pattern.compile(
                "(?i)UPDATE\\s+(\\w+)\\s+SET\\s+(.+?)\\s+WHERE\\s+(\\w+)\\s*=\\s*'?([^'\\s;]+)'?"
        ).matcher(query);
        if (!m.find()) {
            throw new IllegalArgumentException("Cannot parse UPDATE statement");
        }

        String tableName = m.group(1).toLowerCase();
        String setClause = m.group(2);
        String whereCol = m.group(3).toLowerCase();
        String whereVal = m.group(4);

        List<Map<String, Object>> table = DATABASE.get(tableName);
        if (table == null) return 0;

        // Parse SET assignments
        Map<String, Object> updates = new LinkedHashMap<>();
        for (String assignment : setClause.split(",")) {
            String[] parts = assignment.split("=");
            if (parts.length == 2) {
                String col = parts[0].trim().toLowerCase();
                String val = parts[1].trim().replaceAll("^'|'$", "");
                updates.put(col, parseValue(val));
            }
        }

        int count = 0;
        for (Map<String, Object> row : table) {
            Object cellVal = row.get(whereCol);
            if (cellVal != null && cellVal.toString().equalsIgnoreCase(whereVal)) {
                row.putAll(updates);
                count++;
            }
        }
        return count;
    }

    private int executeDelete(String query) {
        // DELETE FROM <table> WHERE col = val
        Matcher m = Pattern.compile(
                "(?i)DELETE\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(\\w+)\\s*=\\s*'?([^'\\s;]+)'?"
        ).matcher(query);
        if (!m.find()) {
            throw new IllegalArgumentException("Cannot parse DELETE statement");
        }

        String tableName = m.group(1).toLowerCase();
        String whereCol = m.group(2).toLowerCase();
        String whereVal = m.group(3);

        List<Map<String, Object>> table = DATABASE.get(tableName);
        if (table == null) return 0;

        int before = table.size();
        table.removeIf(row -> {
            Object cellVal = row.get(whereCol);
            return cellVal != null && cellVal.toString().equalsIgnoreCase(whereVal);
        });
        return before - table.size();
    }

    // ---- helpers ---------------------------------------------------------------

    private Object parseValue(String val) {
        try {
            if (val.contains(".")) return Double.parseDouble(val);
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return val;
        }
    }

    private TaskResult errorResult(Task task, String message) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", List.of(Map.of("error", message)));
        result.getOutputData().put("rowCount", 0);
        result.getOutputData().put("executionTimeMs", 0);
        return result;
    }

    /** Seeds the in-memory database with sample data if not already done. */
    static void ensureSeeded() {
        if (!seeded) {
            synchronized (DATABASE) {
                if (!seeded) {
                    List<Map<String, Object>> departments = new ArrayList<>();
                    departments.add(newRow("Engineering", 45, 2850000, 63333));
                    departments.add(newRow("Sales", 32, 2340000, 73125));
                    departments.add(newRow("Marketing", 28, 1920000, 68571));
                    departments.add(newRow("Finance", 18, 1560000, 86667));
                    departments.add(newRow("Operations", 22, 1180000, 53636));
                    DATABASE.put("departments", departments);

                    List<Map<String, Object>> employees = new ArrayList<>();
                    employees.add(empRow(1, "Alice", "Engineering", 120000));
                    employees.add(empRow(2, "Bob", "Sales", 95000));
                    employees.add(empRow(3, "Carol", "Marketing", 105000));
                    employees.add(empRow(4, "Dave", "Finance", 110000));
                    employees.add(empRow(5, "Eve", "Operations", 90000));
                    DATABASE.put("employees", employees);

                    seeded = true;
                }
            }
        }
    }

    /** Resets the database for testing purposes. */
    static void resetDatabase() {
        synchronized (DATABASE) {
            DATABASE.clear();
            seeded = false;
        }
    }

    private static Map<String, Object> newRow(String dept, int empCount, int revenue, int avgSale) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("department", dept);
        row.put("employee_count", empCount);
        row.put("total_revenue", revenue);
        row.put("avg_sale", avgSale);
        return row;
    }

    private static Map<String, Object> empRow(int id, String name, String dept, int salary) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("department", dept);
        row.put("salary", salary);
        return row;
    }
}
