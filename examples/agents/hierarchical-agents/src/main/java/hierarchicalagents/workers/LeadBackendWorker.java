package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend team lead that receives the backend workstream and breaks it down
 * into an API task and a database task for the backend workers.
 */
public class LeadBackendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_lead_backend";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> workstream = (Map<String, Object>) task.getInputData().get("workstream");

        String scope = workstream != null ? (String) workstream.get("scope") : "Backend development";
        List<String> endpoints = workstream != null ? (List<String>) workstream.get("endpoints") : List.of();

        System.out.println("  [hier_lead_backend] Breaking down: " + scope);

        Map<String, Object> apiTask = new LinkedHashMap<>();
        apiTask.put("endpoints", endpoints);
        apiTask.put("auth", "JWT");
        apiTask.put("validation", "schema-based");

        Map<String, Object> dbTask = new LinkedHashMap<>();
        dbTask.put("tables", List.of("users", "projects", "tasks"));
        dbTask.put("orm", "JPA");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", "Backend lead assigned API and DB tasks for: " + scope);
        result.getOutputData().put("apiTask", apiTask);
        result.getOutputData().put("dbTask", dbTask);
        return result;
    }
}
