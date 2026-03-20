package goaldecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes the third subgoal: evaluating infrastructure scaling options.
 */
public class Subgoal3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_subgoal_3";
    }

    @Override
    public TaskResult execute(Task task) {
        String subgoal = (String) task.getInputData().get("subgoal");
        if (subgoal == null || subgoal.isBlank()) {
            subgoal = "";
        }

        Object indexObj = task.getInputData().get("index");
        int index = (indexObj instanceof Number) ? ((Number) indexObj).intValue() : 2;

        System.out.println("  [gd_subgoal_3] Executing subgoal " + index + ": " + subgoal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result",
                "Horizontal pod autoscaling + read replicas can handle 3x current load at 20% cost increase");
        result.getOutputData().put("status", "complete");
        return result;
    }
}
