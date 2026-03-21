package databaseintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Transforms queried rows.
 * Input: rows, rules
 * Output: transformedRows, transformedCount
 */
public class TransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbi_transform";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> rows =
                (java.util.List<java.util.Map<String, Object>>) task.getInputData().getOrDefault("rows", java.util.List.of());

        java.util.List<java.util.Map<String, Object>> transformed = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> row : rows) {
            java.util.Map<String, Object> t = new java.util.HashMap<>(row);
            Object name = t.get("name");
            if (name instanceof String) {
                t.put("name", ((String) name).toUpperCase());
            }
            t.put("migratedAt", "" + java.time.Instant.now().toString());
            transformed.add(t);
        }
        System.out.println("  [transform] Transformed " + transformed.size() + " rows");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformedRows", transformed);
        result.getOutputData().put("transformedCount", transformed.size());
        return result;
    }
}
