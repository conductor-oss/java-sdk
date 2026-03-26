package logprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses raw log entries into structured format.
 * Input: rawLogs, filters
 * Output: entries, parsedCount, errorCount, warnCount
 */
public class ParseEntriesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lp_parse_entries";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> logs = (List<Map<String, Object>>) task.getInputData().get("rawLogs");
        if (logs == null) {
            logs = List.of();
        }

        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<String, Object> log : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", log.get("ts"));
            entry.put("level", log.get("level"));
            entry.put("service", log.get("service"));
            entry.put("message", log.get("msg"));
            entry.put("isError", "ERROR".equals(log.get("level")));
            entries.add(entry);
        }

        long errors = entries.stream().filter(e -> Boolean.TRUE.equals(e.get("isError"))).count();
        long warns = entries.stream().filter(e -> "WARN".equals(e.get("level"))).count();

        System.out.println("  [parse] Parsed " + entries.size() + " entries — " + errors + " errors, " + warns + " warnings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("entries", entries);
        result.getOutputData().put("parsedCount", entries.size());
        result.getOutputData().put("errorCount", (int) errors);
        result.getOutputData().put("warnCount", (int) warns);
        return result;
    }
}
