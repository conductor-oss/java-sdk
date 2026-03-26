package nutritiontracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class LogMealWorker implements Worker {
    @Override public String getTaskDefName() { return "nut_log_meal"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [log] Logging " + task.getInputData().get("mealType") + " for user " + task.getInputData().get("userId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("loggedFoods", List.of("Grilled Chicken", "Brown Rice", "Broccoli"));
        result.addOutputData("mealId", "MEAL-301");
        return result;
    }
}
