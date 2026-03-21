package goaldecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Decomposes a high-level goal into three concrete subgoals.
 */
public class DecomposeGoalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_decompose_goal";
    }

    @Override
    public TaskResult execute(Task task) {
        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "";
        }

        System.out.println("  [gd_decompose_goal] Decomposing goal: " + goal);

        List<String> subgoals = List.of(
                "Analyze current system performance bottlenecks",
                "Research caching and optimization strategies",
                "Evaluate infrastructure scaling options"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subgoals", subgoals);
        return result;
    }
}
