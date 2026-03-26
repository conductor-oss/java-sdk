package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manager agent that merges backend and frontend results into a final project report,
 * computing total lines of code across all workers.
 */
public class ManagerMergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_manager_merge";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> backendResults = (Map<String, Object>) task.getInputData().get("backendResults");
        Map<String, Object> frontendResults = (Map<String, Object>) task.getInputData().get("frontendResults");

        System.out.println("  [hier_manager_merge] Merging backend and frontend results");

        int backendLines = extractLines(backendResults, "apiResult") + extractLines(backendResults, "dbResult");
        int frontendLines = extractLines(frontendResults, "uiResult") + extractLines(frontendResults, "stylingResult");
        int totalLines = backendLines + frontendLines;

        Map<String, Object> projectReport = new LinkedHashMap<>();
        projectReport.put("status", "completed");
        projectReport.put("backend", backendResults);
        projectReport.put("frontend", frontendResults);
        projectReport.put("totalLinesOfCode", totalLines);
        projectReport.put("hierarchy", Map.of(
                "manager", 1,
                "leads", 2,
                "workers", 4
        ));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("projectReport", projectReport);
        return result;
    }

    private int extractLines(Map<String, Object> results, String key) {
        if (results == null) return 0;
        Object val = results.get(key);
        if (val instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) val;
            Object loc = map.get("linesOfCode");
            if (loc instanceof Number) {
                return ((Number) loc).intValue();
            }
        }
        return 0;
    }
}
