package menumanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CreateItemsWorker implements Worker {
    @Override public String getTaskDefName() { return "mnu_create_items"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [create] Creating menu items for restaurant " + task.getInputData().get("restaurantId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("items", List.of(
            Map.of("name", "Grilled Salmon", "description", "Atlantic salmon with herbs"),
            Map.of("name", "Truffle Pasta", "description", "Fresh pasta with truffle cream"),
            Map.of("name", "Tiramisu", "description", "Classic Italian dessert")
        ));
        return result;
    }
}
