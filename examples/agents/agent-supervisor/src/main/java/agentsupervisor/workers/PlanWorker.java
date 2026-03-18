package agentsupervisor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that creates a development plan for a feature.
 * Takes feature name, priority, and system prompt as inputs.
 * Returns a plan with phases and deadline, plus task assignments
 * for coding, testing, and documentation agents.
 */
public class PlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sup_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().get("feature");
        String priority = (String) task.getInputData().get("priority");

        if (feature == null) feature = "user-authentication";
        if (priority == null) priority = "high";

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("feature", feature);
        plan.put("priority", priority);
        plan.put("phases", List.of("design", "implementation", "testing", "documentation", "review"));
        plan.put("deadline", "2026-03-21");

        String codingTask = "Implement " + feature + " with all required endpoints and business logic";
        String testingTask = "Create comprehensive test suites for " + feature + " covering unit, integration, and edge cases";
        String documentationTask = "Write complete documentation for " + feature + " including API reference, guides, and examples";

        System.out.println("  [plan] Created plan for feature '" + feature + "' with priority '" + priority + "'");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("plan", plan);
        result.getOutputData().put("codingTask", codingTask);
        result.getOutputData().put("testingTask", testingTask);
        result.getOutputData().put("documentationTask", documentationTask);
        return result;
    }
}
