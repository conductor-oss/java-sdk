package agenticloop.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Initializes the agentic loop by accepting a goal and marking it as active.
 * Input:  {goal}
 * Output: {goal, status:"active"}
 */
public class SetGoalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "al_set_goal";
    }

    @Override
    public TaskResult execute(Task task) {
        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "No goal specified";
        }

        System.out.println("  [al_set_goal] Setting goal: " + goal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("goal", goal);
        result.getOutputData().put("status", "active");
        return result;
    }
}
