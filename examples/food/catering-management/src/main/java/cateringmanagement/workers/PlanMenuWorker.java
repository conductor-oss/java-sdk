package cateringmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class PlanMenuWorker implements Worker {
    @Override public String getTaskDefName() { return "cat_plan_menu"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [menu] Planning menu for " + task.getInputData().get("guestCount") + " guests, budget $" + task.getInputData().get("budget"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("menu", Map.of("appetizers", 3, "mains", 4, "desserts", 2, "beverages", true));
        return result;
    }
}
