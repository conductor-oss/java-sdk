package foodsafety.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CheckTempsWorker implements Worker {
    @Override public String getTaskDefName() { return "fsf_check_temps"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [temps] Checking storage temperatures at " + task.getInputData().get("restaurantId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("temps", Map.of("fridge", 38, "freezer", -2, "hotHold", 145, "allPass", true));
        return result;
    }
}
