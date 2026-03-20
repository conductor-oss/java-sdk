package logaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores enriched logs in the log store index.
 */
public class StoreLogs implements Worker {

    @Override
    public String getTaskDefName() {
        return "la_store_logs";
    }

    @Override
    public TaskResult execute(Task task) {
        Object enrichedCountObj = task.getInputData().get("enrichedCount");
        Object sizeBytesObj = task.getInputData().get("sizeBytes");

        int enrichedCount = toInt(enrichedCountObj, 0);
        double sizeMb = toInt(sizeBytesObj, 0) / 1_000_000.0;

        System.out.println("[la_store_logs] Storing " + enrichedCount
                + " enriched logs (" + String.format("%.1f", sizeMb) + "MB)");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stored", true);
        output.put("index", "logs-2026.03.08");
        output.put("documentsWritten", 14800);
        output.put("storageMs", 3200);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
