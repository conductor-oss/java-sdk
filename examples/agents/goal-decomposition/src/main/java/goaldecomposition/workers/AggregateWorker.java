package goaldecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Aggregates the results from all three subgoal workers into a single summary.
 */
public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gd_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        String goal = (String) task.getInputData().get("goal");
        if (goal == null || goal.isBlank()) {
            goal = "";
        }

        String result1 = (String) task.getInputData().get("result1");
        if (result1 == null) result1 = "";

        String result2 = (String) task.getInputData().get("result2");
        if (result2 == null) result2 = "";

        String result3 = (String) task.getInputData().get("result3");
        if (result3 == null) result3 = "";

        System.out.println("  [gd_aggregate] Aggregating results for goal: " + goal);

        String aggregatedResult = result1 + "; " + result2 + "; " + result3;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregatedResult", aggregatedResult);
        return result;
    }
}
