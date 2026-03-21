package goaldecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes the second subgoal: researching caching and optimization strategies.
 */
public class Subgoal2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_subgoal_2";
    }

    @Override
    public TaskResult execute(Task task) {
        String subgoal = (String) task.getInputData().get("subgoal");
        if (subgoal == null || subgoal.isBlank()) {
            subgoal = "";
        }

        Object indexObj = task.getInputData().get("index");
        int index = (indexObj instanceof Number) ? ((Number) indexObj).intValue() : 1;

        System.out.println("  [gd_subgoal_2] Executing subgoal " + index + ": " + subgoal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result",
                "Recommended Redis caching (70% hit rate expected), query optimization, and response compression");
        result.getOutputData().put("status", "complete");
        return result;
    }
}
