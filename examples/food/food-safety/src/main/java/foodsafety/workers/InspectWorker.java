package foodsafety.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class InspectWorker implements Worker {
    @Override public String getTaskDefName() { return "fsf_inspect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [inspect] Inspector " + task.getInputData().get("inspectorId") + " inspecting restaurant " + task.getInputData().get("restaurantId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("findings", Map.of("cleanliness", "A", "equipment", "good", "violations", 0));
        return result;
    }
}
