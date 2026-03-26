package nutritiontracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "nut_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Daily intake report generated");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("report", Map.of(
            "userId", task.getInputData().getOrDefault("userId", "USR-55"),
            "calories", 1650, "goal", 2000, "remaining", 500, "status", "ON_TRACK"));
        return result;
    }
}
