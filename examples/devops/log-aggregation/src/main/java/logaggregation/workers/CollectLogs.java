package logaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects raw logs from the specified sources for the given time range.
 */
public class CollectLogs implements Worker {

    @Override
    public String getTaskDefName() {
        return "la_collect_logs";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object sourcesObj = task.getInputData().get("sources");
        String timeRange = (String) task.getInputData().get("timeRange");

        int sourceCount = 3;
        if (sourcesObj instanceof List) {
            sourceCount = ((List<Object>) sourcesObj).size();
        }

        System.out.println("[la_collect_logs] Collecting logs from " + sourceCount
                + " sources (" + timeRange + ")");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("rawLogCount", 15000);
        output.put("format", "mixed");
        output.put("sources", sourcesObj);
        output.put("collectedAt", "2026-03-08T06:00:00Z");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
