package foodordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class BrowseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fod_browse";
    }

    @Override
    public TaskResult execute(Task task) {
        String restaurantId = (String) task.getInputData().get("restaurantId");
        System.out.println("  [browse] Browsing menu for restaurant " + restaurantId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("selectedItems", List.of(
                Map.of("name", "Margherita Pizza", "price", 14.99, "qty", 1),
                Map.of("name", "Caesar Salad", "price", 9.99, "qty", 1)
        ));
        return result;
    }
}
