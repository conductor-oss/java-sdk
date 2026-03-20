package menumanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class PriceWorker implements Worker {
    @Override public String getTaskDefName() { return "mnu_price"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [price] Setting prices for items");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("pricedItems", List.of(
            Map.of("name", "Grilled Salmon", "price", 28.99),
            Map.of("name", "Truffle Pasta", "price", 22.99),
            Map.of("name", "Tiramisu", "price", 12.99)
        ));
        return result;
    }
}
