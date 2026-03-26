package goaldecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes the first subgoal: analyzing current system performance bottlenecks.
 */
public class Subgoal1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_subgoal_1";
    }

    @Override
    public TaskResult execute(Task task) {
        String subgoal = (String) task.getInputData().get("subgoal");
        if (subgoal == null || subgoal.isBlank()) {
            subgoal = "";
        }

        Object indexObj = task.getInputData().get("index");
        int index = (indexObj instanceof Number) ? ((Number) indexObj).intValue() : 0;

        System.out.println("  [gd_subgoal_1] Executing subgoal " + index + ": " + subgoal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result",
                "Identified 3 bottlenecks: database queries (40%), API serialization (25%), network latency (15%)");
        result.getOutputData().put("status", "complete");
        return result;
    }
}
