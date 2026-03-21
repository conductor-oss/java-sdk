package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager agent that takes a project description and deadline, then produces
 * a backend plan and a frontend plan for the team leads.
 */
public class ManagerPlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_manager_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        String project = (String) task.getInputData().get("project");
        String deadline = (String) task.getInputData().get("deadline");

        if (project == null || project.isBlank()) {
            project = "Default Project";
        }
        if (deadline == null || deadline.isBlank()) {
            deadline = "2 weeks";
        }

        System.out.println("  [hier_manager_plan] Planning project: " + project + " (deadline: " + deadline + ")");

        Map<String, Object> backendPlan = new LinkedHashMap<>();
        backendPlan.put("scope", "Build REST API and database layer for " + project);
        backendPlan.put("endpoints", List.of("/api/users", "/api/projects", "/api/tasks"));
        backendPlan.put("priority", "high");

        Map<String, Object> frontendPlan = new LinkedHashMap<>();
        frontendPlan.put("scope", "Build user interface for " + project);
        frontendPlan.put("pages", List.of("Dashboard", "ProjectView", "Settings"));
        frontendPlan.put("priority", "high");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("backendPlan", backendPlan);
        result.getOutputData().put("frontendPlan", frontendPlan);
        return result;
    }
}
