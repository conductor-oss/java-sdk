package planexecuteagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Creates a multi-step execution plan from a high-level objective.
 * Returns the list of steps and the total step count.
 */
public class CreatePlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pe_create_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        String objective = (String) task.getInputData().get("objective");
        if (objective == null || objective.isBlank()) {
            objective = "";
        }

        System.out.println("  [pe_create_plan] Creating plan for objective: " + objective);

        List<String> steps = List.of(
                "Gather market data and competitor analysis",
                "Analyze trends and identify opportunities",
                "Generate strategic recommendations"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("steps", steps);
        result.getOutputData().put("totalSteps", 3);
        return result;
    }
}
