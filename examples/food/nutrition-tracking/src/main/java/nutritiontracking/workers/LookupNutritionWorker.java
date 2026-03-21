package nutritiontracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class LookupNutritionWorker implements Worker {
    @Override public String getTaskDefName() { return "nut_lookup_nutrition"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lookup] Looking up nutrition for foods");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("nutrition", Map.of("calories", 520, "protein", 42, "carbs", 55, "fat", 12, "fiber", 8));
        return result;
    }
}
