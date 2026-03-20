package nutritiontracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CalculateDailyWorker implements Worker {
    @Override public String getTaskDefName() { return "nut_calculate_daily"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [calc] Calculating daily totals");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("daily", Map.of("totalCalories", 1650, "totalProtein", 105, "totalCarbs", 180, "totalFat", 52, "goal", 2000));
        return result;
    }
}
