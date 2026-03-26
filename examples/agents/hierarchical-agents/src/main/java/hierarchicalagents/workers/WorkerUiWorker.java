package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI developer worker that builds the page components defined by the frontend lead.
 */
public class WorkerUiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_worker_ui";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> uiTask = (Map<String, Object>) task.getInputData().get("task");

        List<String> pages = uiTask != null ? (List<String>) uiTask.get("pages") : List.of();

        System.out.println("  [hier_worker_ui] Building " + pages.size() + " page components");

        List<Map<String, Object>> components = pages.stream().map(page -> {
            Map<String, Object> component = new LinkedHashMap<>();
            component.put("name", page);
            component.put("status", "implemented");
            return component;
        }).toList();

        Map<String, Object> uiResult = new LinkedHashMap<>();
        uiResult.put("components", components);
        uiResult.put("sharedComponents", List.of("Header", "Footer", "Sidebar", "Loading"));
        uiResult.put("linesOfCode", 450);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", uiResult);
        return result;
    }
}
