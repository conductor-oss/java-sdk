package autonomousagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Creates a multi-step plan from a goal and its constraints.
 * Input:  {goal, constraints}
 * Output: {plan, estimatedSteps}
 */
public class CreatePlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aa_create_plan";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "";
        }

        List<String> constraints = (List<String>) task.getInputData().get("constraints");
        if (constraints == null) {
            constraints = List.of();
        }

        System.out.println("  [aa_create_plan] Goal: " + goal);

        List<String> plan = List.of(
                "Set up metrics collection pipeline (Prometheus + exporters)",
                "Build Grafana dashboard with key panels and thresholds",
                "Configure alert rules and notification channels"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("plan", plan);
        result.getOutputData().put("estimatedSteps", 3);
        return result;
    }
}
