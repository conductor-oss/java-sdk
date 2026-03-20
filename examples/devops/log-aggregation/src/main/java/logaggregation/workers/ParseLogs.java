package logaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses raw logs into a structured format.
 */
public class ParseLogs implements Worker {

    @Override
    public String getTaskDefName() {
        return "la_parse_logs";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawLogCountObj = task.getInputData().get("rawLogCount");
        String format = (String) task.getInputData().get("format");

        int rawLogCount = toInt(rawLogCountObj, 0);
        System.out.println("[la_parse_logs] Parsing " + rawLogCount
                + " raw logs (format: " + format + ")");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("parsedCount", 14800);
        output.put("parseErrors", 200);
        output.put("structuredLogs", "json");
        output.put("avgParseTimeMs", 0.2);

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
