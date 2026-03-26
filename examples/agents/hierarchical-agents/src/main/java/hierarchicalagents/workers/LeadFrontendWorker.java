package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frontend team lead that receives the frontend workstream and breaks it down
 * into a UI task and a styling task for the frontend workers.
 */
public class LeadFrontendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_lead_frontend";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> workstream = (Map<String, Object>) task.getInputData().get("workstream");

        String scope = workstream != null ? (String) workstream.get("scope") : "Frontend development";
        List<String> pages = workstream != null ? (List<String>) workstream.get("pages") : List.of();

        System.out.println("  [hier_lead_frontend] Breaking down: " + scope);

        Map<String, Object> uiTask = new LinkedHashMap<>();
        uiTask.put("pages", pages);
        uiTask.put("framework", "React");
        uiTask.put("stateManagement", "Redux");

        Map<String, Object> stylingTask = new LinkedHashMap<>();
        stylingTask.put("designSystem", "Material UI");
        stylingTask.put("responsive", true);
        stylingTask.put("darkMode", true);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", "Frontend lead assigned UI and styling tasks for: " + scope);
        result.getOutputData().put("uiTask", uiTask);
        result.getOutputData().put("stylingTask", stylingTask);
        return result;
    }
}
