package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API developer worker that implements the REST endpoints defined by the backend lead.
 */
public class WorkerApiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_worker_api";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> apiTask = (Map<String, Object>) task.getInputData().get("task");

        List<String> endpointPaths = apiTask != null ? (List<String>) apiTask.get("endpoints") : List.of();

        System.out.println("  [hier_worker_api] Implementing " + endpointPaths.size() + " endpoints");

        List<Map<String, Object>> endpoints = endpointPaths.stream().map(path -> {
            Map<String, Object> ep = new LinkedHashMap<>();
            ep.put("path", path);
            ep.put("methods", List.of("GET", "POST", "PUT", "DELETE"));
            ep.put("status", "implemented");
            return ep;
        }).toList();

        Map<String, Object> apiResult = new LinkedHashMap<>();
        apiResult.put("endpoints", endpoints);
        apiResult.put("middleware", List.of("auth", "validation", "errorHandler"));
        apiResult.put("linesOfCode", 320);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", apiResult);
        return result;
    }
}
