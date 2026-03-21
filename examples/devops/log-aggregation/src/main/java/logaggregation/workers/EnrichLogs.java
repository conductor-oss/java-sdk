package logaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches parsed logs with additional metadata fields.
 */
public class EnrichLogs implements Worker {

    @Override
    public String getTaskDefName() {
        return "la_enrich_logs";
    }

    @Override
    public TaskResult execute(Task task) {
        Object parsedCountObj = task.getInputData().get("parsedCount");
        int parsedCount = toInt(parsedCountObj, 0);

        System.out.println("[la_enrich_logs] Enriching " + parsedCount
                + " parsed logs with metadata");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("enrichedCount", 14800);
        output.put("sizeBytes", 45000000);
        output.put("fieldsAdded", List.of("geo", "service", "traceId", "userId"));

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
